/*
 * Copyright 2022 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.front50.model;

import static net.logstash.logback.argument.StructuredArguments.value;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.BucketVersioningConfiguration;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.ListVersionsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.SetBucketVersioningRequest;
import com.aliyun.oss.model.VersionListing;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.front50.api.model.Timestamped;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

@Slf4j
public class OssStorageService implements StorageService {
  private final ObjectMapper objectMapper;
  private final OSS oss;
  private final String bucket;
  private final String rootFolder;
  private final Boolean versioning;
  private final Boolean readOnlyMode;
  private final Integer maxKeys;

  public OssStorageService(
      ObjectMapper objectMapper,
      OSS oss,
      String bucket,
      String rootFolder,
      Boolean versioning,
      Boolean readOnlyMode,
      Integer maxKeys) {
    this.objectMapper = objectMapper;
    this.oss = oss;
    this.bucket = bucket;
    this.rootFolder = rootFolder;
    this.versioning = versioning;
    this.readOnlyMode = readOnlyMode;
    this.maxKeys = maxKeys;
  }

  public void ensureBucketExists() {
    try {
      oss.getBucketInfo(bucket);
    } catch (OSSException e) {
      if ("NoSuchBucket".equals(e.getErrorCode())) {
        log.info("NoSuchBucket create bucket:{}", bucket);
        oss.createBucket(bucket);
        if (versioning) {
          log.info("Enabling versioning of the oss bucket {}", bucket);
          BucketVersioningConfiguration configuration =
              new BucketVersioningConfiguration().withStatus("Enabled");

          SetBucketVersioningRequest setBucketVersioningRequest =
              new SetBucketVersioningRequest(bucket, configuration);

          oss.setBucketVersioning(setBucketVersioningRequest);
        }
      } else {
        throw e;
      }
    }
  }

  @Override
  public boolean supportsVersioning() {
    return Boolean.TRUE.equals(versioning);
  }

  @Override
  public <T extends Timestamped> T loadObject(ObjectType objectType, String objectKey)
      throws NotFoundException {
    try {
      OSSObject ossObject =
          oss.getObject(
              bucket, buildOssKey(objectType.group, objectKey, objectType.defaultMetadataFilename));
      T item = deserialize(ossObject, (Class<T>) objectType.clazz);
      item.setLastModified(ossObject.getObjectMetadata().getLastModified().getTime());
      return item;
    } catch (OSSException e) {
      if ("NoSuchKey".equals(e.getErrorCode())) {
        throw new NotFoundException();
      }
      log.error("loadObject error,objectType:{}, objectKey:{}", objectType, objectKey, e);
      throw e;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to deserialize object (key: " + objectKey + ")", e);
    }
  }

  @Override
  public void deleteObject(ObjectType objectType, String objectKey) {
    checkReadOnly();
    oss.deleteObject(
        bucket, buildOssKey(objectType.group, objectKey, objectType.defaultMetadataFilename));
    writeLastModified(objectType.group);
  }

  @Override
  public <T extends Timestamped> void storeObject(ObjectType objectType, String objectKey, T item) {
    checkReadOnly();
    try {
      byte[] bytes = objectMapper.writeValueAsBytes(item);

      ObjectMetadata objectMetadata = new ObjectMetadata();
      objectMetadata.setContentLength(bytes.length);
      objectMetadata.setContentMD5(
          new String(org.apache.commons.codec.binary.Base64.encodeBase64(DigestUtils.md5(bytes))));

      oss.putObject(
          bucket,
          buildOssKey(objectType.group, objectKey, objectType.defaultMetadataFilename),
          new ByteArrayInputStream(bytes),
          objectMetadata);
      writeLastModified(objectType.group);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Map<String, Long> listObjectKeys(ObjectType objectType) {
    long startTime = System.currentTimeMillis();
    String nextMarker = null;
    ObjectListing objectListing;
    List<OSSObjectSummary> summaries = new ArrayList<>(maxKeys * 3);
    do {
      objectListing =
          oss.listObjects(
              new ListObjectsRequest(bucket).withMarker(nextMarker).withMaxKeys(maxKeys));
      summaries.addAll(objectListing.getObjectSummaries());
      nextMarker = objectListing.getNextMarker();
    } while (objectListing.isTruncated());

    log.debug(
        "Took {}ms to fetch {} object keys for {}",
        value("fetchTime", (System.currentTimeMillis() - startTime)),
        summaries.size(),
        value("type", objectType));

    return summaries.stream()
        .filter(s -> filterObjectSummary(s, objectType.defaultMetadataFilename))
        .collect(
            Collectors.toMap(
                (s -> buildObjectKey(objectType, s.getKey())),
                (s -> s.getLastModified().getTime())));
  }

  @Override
  public <T extends Timestamped> Collection<T> listObjectVersions(
      ObjectType objectType, String objectKey, int maxResults)
      throws com.netflix.spinnaker.kork.web.exceptions.NotFoundException {
    if (maxResults == 1) {
      List<T> results = new ArrayList<>();
      results.add(loadObject(objectType, objectKey));
      return results;
    }

    try {
      VersionListing versionListing =
          oss.listVersions(
              new ListVersionsRequest(
                  bucket,
                  buildOssKey(objectType.group, objectKey, objectType.defaultMetadataFilename),
                  null,
                  null,
                  null,
                  maxResults));
      return versionListing.getVersionSummaries().stream()
          .map(
              versionSummary -> {
                try {
                  OSSObject ossObject =
                      oss.getObject(
                          new GetObjectRequest(
                              bucket,
                              buildOssKey(
                                  objectType.group, objectKey, objectType.defaultMetadataFilename),
                              versionSummary.getVersionId()));
                  T item = deserialize(ossObject, (Class<T>) objectType.clazz);
                  item.setLastModified(ossObject.getObjectMetadata().getLastModified().getTime());
                  return item;
                } catch (IOException e) {
                  throw new IllegalStateException(e);
                }
              })
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("", e);
      throw e;
    }
  }

  @Override
  public long getLastModified(ObjectType objectType) {
    try {
      Map<String, Long> lastModified =
          objectMapper.readValue(
              oss.getObject(
                      bucket,
                      buildTypedFolder(rootFolder, objectType.group) + "/last-modified.json")
                  .getObjectContent(),
              Map.class);

      return lastModified.get("lastModified");
    } catch (Exception e) {
      return 0L;
    }
  }

  private void checkReadOnly() {
    if (readOnlyMode) {
      throw new RuntimeException("Cannot perform write operation, in read-only mode");
    }
  }

  private void writeLastModified(String group) {
    checkReadOnly();
    try {
      byte[] bytes =
          objectMapper.writeValueAsBytes(
              Collections.singletonMap("lastModified", System.currentTimeMillis()));

      ObjectMetadata objectMetadata = new ObjectMetadata();
      objectMetadata.setContentLength(bytes.length);
      objectMetadata.setContentMD5(
          new String(org.apache.commons.codec.binary.Base64.encodeBase64(DigestUtils.md5(bytes))));
      oss.putObject(
          bucket,
          buildTypedFolder(rootFolder, group) + "/last-modified.json",
          new ByteArrayInputStream(bytes),
          objectMetadata);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean filterObjectSummary(OSSObjectSummary objectSummary, String metadataFilename) {
    return objectSummary.getKey().endsWith(metadataFilename);
  }

  private <T extends Timestamped> T deserialize(OSSObject ossObject, Class<T> clazz)
      throws IOException {
    return objectMapper.readValue(ossObject.getObjectContent(), clazz);
  }

  private String buildOssKey(String group, String objectKey, String metadataFilename) {
    if (objectKey.endsWith(metadataFilename)) {
      return objectKey;
    }

    return (buildTypedFolder(rootFolder, group)
            + "/"
            + objectKey.toLowerCase()
            + "/"
            + metadataFilename)
        .replace("//", "/");
  }

  private String buildObjectKey(ObjectType objectType, String s3Key) {
    return s3Key
        .replaceAll(buildTypedFolder(rootFolder, objectType.group) + "/", "")
        .replaceAll("/" + objectType.defaultMetadataFilename, "");
  }

  private static String buildTypedFolder(String rootFolder, String type) {
    return (rootFolder + "/" + type).replaceAll("//", "/");
  }
}
