/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.front50.model;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.spinnaker.front50.exception.NotFoundException;

import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.*;

import com.google.api.services.storage.model.Bucket;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.util.DateTime;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GcsStorageService implements StorageService {
  private static final String DEFAULT_DATA_FILENAME = "specification.json";
  private static final String LAST_MODIFIED_FILENAME = "last-modified";
  private final Logger log = LoggerFactory.getLogger(getClass());

  private ObjectMapper objectMapper = new ObjectMapper();
  private String projectName;
  private String bucketName;
  private String basePath;
  private Storage storage;
  private Storage.Objects obj_api;
  private String dataFilename = DEFAULT_DATA_FILENAME;

  /**
   * Bucket location for when a missing bucket is created. Has no effect if the bucket already
   * exists.
   */
  private String bucketLocation;

  public Storage getStorage() { return this.storage; }
  public ObjectMapper getObjectMapper() { return this.objectMapper; }

  private GoogleCredential loadCredential(
      HttpTransport transport, JsonFactory factory, String jsonPath)
      throws IOException {
    GoogleCredential credential;
    if (!jsonPath.isEmpty()) {
      FileInputStream stream = new FileInputStream(jsonPath);
      credential = GoogleCredential.fromStream(stream, transport, factory)
                      .createScoped(Collections.singleton(StorageScopes.DEVSTORAGE_FULL_CONTROL));
      log.info("Loaded credentials from from " + jsonPath);
    } else {
      log.info("spinnaker.gcs.enabled without spinnaker.gcs.jsonPath. " +
                   "Using default application credentials. Using default credentials.");
      credential = GoogleCredential.getApplicationDefault();
    }
    return credential;
  }

  @VisibleForTesting
  GcsStorageService(String bucketName,
                    String bucketLocation,
                    String basePath,
                    String projectName,
                    Storage storage) {
    this.bucketName = bucketName;
    this.bucketLocation = bucketLocation;
    this.basePath = basePath;
    this.projectName = projectName;
    this.storage = storage;
    this.obj_api = storage.objects();
  }

  public GcsStorageService(String bucketName,
                           String bucketLocation,
                           String basePath,
                           String projectName,
                           String credentialsPath,
                           String applicationVersion) {
    this(bucketName,
         bucketLocation,
         basePath,
         projectName,
         credentialsPath,
         applicationVersion,
         DEFAULT_DATA_FILENAME);
  }

  public GcsStorageService(String bucketName,
                           String bucketLocation,
                           String basePath,
                           String projectName,
                           String credentialsPath,
                           String applicationVersion,
                           String dataFilename) {
    Storage storage;

    try {
      HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      GoogleCredential credential = loadCredential(httpTransport, jsonFactory,
                                                   credentialsPath);

      String applicationName = "Spinnaker-front50/" + applicationVersion;
      storage = new Storage.Builder(httpTransport, jsonFactory, credential)
                           .setApplicationName(applicationName)
                           .build();
    } catch (IOException|java.security.GeneralSecurityException e) {
        throw new IllegalStateException(e);
    }

    this.bucketName = bucketName;
    this.bucketLocation = bucketLocation;
    this.basePath = basePath;
    this.projectName = projectName;
    this.storage = storage;
    this.obj_api = this.storage.objects();
    this.dataFilename = dataFilename;
  }

  /**
   * Check to see if the bucket exists, creating it if it is not there.
   */
  public void ensureBucketExists() {
    try {
      Bucket bucket = storage.buckets().get(bucketName).execute();
    } catch (HttpResponseException e) {
      if (e.getStatusCode() == 404) {
        log.warn("Bucket {} does not exist. Creating it in project={}",
                 bucketName, projectName);
        Bucket.Versioning versioning = new Bucket.Versioning().setEnabled(true);
        Bucket bucket = new Bucket().setName(bucketName).setVersioning(versioning);
        if (StringUtils.isNotBlank(bucketLocation)) {
          bucket.setLocation(bucketLocation);
        }
        try {
            storage.buckets().insert(projectName, bucket).execute();
        } catch (IOException e2) {
            log.error("Could not create bucket={} in project={}: {}",
                      bucketName, projectName, e2);
            throw new IllegalStateException(e2);
        }
      } else {
          log.error("Could not get bucket={}: {}", bucketName, e);
          throw new IllegalStateException(e);
      }
    } catch (IOException e) {
        log.error("Could not get bucket={}: {}", bucketName, e);
        throw new IllegalStateException(e);
    }
  }

  /**
   * Returns true if the storage service supports versioning.
   */
  public boolean supportsVersioning() {
    try {
      Bucket bucket = storage.buckets().get(bucketName).execute();
      Bucket.Versioning v = bucket.getVersioning();
      return v != null && v.getEnabled();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public <T extends Timestamped> T
         loadCurrentObject(String objectKey, String daoTypeName, Class<T> clas)
         throws NotFoundException {
    String path = keyToPath(objectKey, daoTypeName);
    try {
      StorageObject storageObject = obj_api.get(bucketName, path).execute();
      T item = deserialize(storageObject, clas, true);
      item.setLastModified(storageObject.getUpdated().getValue());
      log.debug("Loaded bucket={} path={}", bucketName, path);
      return item;
    } catch (HttpResponseException e) {
      log.error("Failed to load {} {}: {} {}",
                daoTypeName, objectKey, e.getStatusCode(), e.getStatusMessage());
      if (e.getStatusCode() == 404) {
          throw new NotFoundException(String.format("No file at path=%s", path));
      }
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new IllegalStateException(e);

    }
  }

  public <T extends Timestamped> T
         loadObjectVersion(String objectKey, String daoTypeName, Class<T> clas,
                           String versionId) throws NotFoundException {
      return null;
  }

  public void deleteObject(String objectKey, String daoTypeName) {
    String path = keyToPath(objectKey, daoTypeName);
    try {
      obj_api.delete(bucketName, path).execute();
      writeLastModified(daoTypeName);
    } catch (IOException e) {
      log.error("Delete failed on path={}", path);
      throw new IllegalStateException(e);
    }
  }

  public <T extends Timestamped>
         void storeObject(String objectKey, String daoTypeName, T obj) {
    String path = keyToPath(objectKey, daoTypeName);
    try {
      byte[] bytes = objectMapper.writeValueAsBytes(obj);
      StorageObject object = new StorageObject().setBucket(bucketName).setName(path);
      ByteArrayContent content = new ByteArrayContent("application/json", bytes);
      obj_api.insert(bucketName, object, content).execute();
      writeLastModified(daoTypeName);
    } catch (IOException e) {
      log.error("Update failed on path={}: {}", path, e);
      throw new IllegalStateException(e);
    }
  }

  public Map<String, Long> listObjectKeys(String daoTypeName) {
    String rootFolder = daoRoot(daoTypeName);
    int skipToOffset = rootFolder.length() + 1;  // + Trailing slash
    int skipFromEnd = dataFilename.length() + 1;  // + Leading slash

    Map<String, Long> result = new HashMap<String, Long>();
    log.debug("Listing {}", daoTypeName);
    try {
      Storage.Objects.List listObjects = obj_api.list(bucketName);
      listObjects.setPrefix(rootFolder);
      com.google.api.services.storage.model.Objects objects;
      do {
          objects = listObjects.execute();
          List<StorageObject> items = objects.getItems();
          if (items != null) {
              for (StorageObject item: items) {
                  String name = item.getName();
                  if (name.endsWith(dataFilename)) {
                      result.put(name.substring(skipToOffset,
                                                name.length() - skipFromEnd),
                                 item.getUpdated().getValue());
                  }
              }
          }
          listObjects.setPageToken(objects.getNextPageToken());
      } while (objects.getNextPageToken() != null);
    } catch (IOException e) {
      log.error("Could not fetch items from Google Cloud Storage: {}", e);
      return new HashMap<String, Long>();
    }

    return result;
  }

  public <T extends Timestamped> Collection<T>
         listObjectVersions(String objectKey, String daoTypeName, Class<T> clas,
                            int maxResults) throws NotFoundException {
    String path = keyToPath(objectKey, daoTypeName);
    ArrayList<T> result = new ArrayList<T>();
    try {
      Storage.Objects.List listObjects = obj_api.list(bucketName)
          .setPrefix(path)
          .setVersions(true)
          .setMaxResults(new Long(maxResults));
      com.google.api.services.storage.model.Objects objects;
      do {
          objects = listObjects.execute();
          List<StorageObject> items = objects.getItems();
          if (items != null) {
            for (StorageObject item : items) {
                T have = deserialize(item, clas, false);
                if (have != null) {
                  result.add(have);
                }
            }
          }
          listObjects.setPageToken(objects.getNextPageToken());
      } while (objects.getNextPageToken() != null);
    } catch (IOException e) {
      log.error("Could not fetch versions from Google Cloud Storage: {}", e);
      return new ArrayList<>();
    }
    return result;
  }

  private <T extends Timestamped> T
          deserialize(StorageObject object, Class<T> clas, boolean current_version)
      throws java.io.UnsupportedEncodingException {
    try {
        ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        Storage.Objects.Get getter = obj_api.get(object.getBucket(), object.getName());
        if (!current_version) {
            getter.setGeneration(object.getGeneration());
        }
        getter.executeMediaAndDownloadTo(output);
        String json = output.toString("UTF8");
        return objectMapper.readValue(json, clas);
      } catch (IOException ex) {
        if (current_version) {
          log.error("Error reading {}: {}", object.getName(), ex);
        } else {
          log.error("Error reading {} generation={}: {}",
                    object.getName(), object.getGeneration(), ex);
        }
        return null;
    }
  }

  private void writeLastModified(String daoTypeName) {
      // We'll just touch the file since the StorageObject manages a timestamp.
      String timestamp_path = daoRoot(daoTypeName) + '/' + LAST_MODIFIED_FILENAME;
      StorageObject object = new StorageObject()
          .setBucket(bucketName)
          .setName(timestamp_path)
          .setUpdated(new DateTime(System.currentTimeMillis()));
      try {
        obj_api.update(bucketName, object.getName(), object).execute();
      } catch (HttpResponseException e) {
        log.error("writeLastModified failed to update: {}", e.toString());

        if (e.getStatusCode() == 404 || e.getStatusCode() == 400) {
            byte[] bytes = "{}".getBytes();
            ByteArrayContent content = new ByteArrayContent("application/json", bytes);

            try {
              obj_api.insert(bucketName, object, content).execute();
            } catch (IOException ioex) {
              log.error("writeLastModified insert failed too: {}", ioex);
              throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalStateException(e);
        }
      } catch (IOException e) {
        log.error("writeLastModified failed: {}", e);
        throw new IllegalStateException(e);
      }

      try {
          // If the bucket is versioned, purge the old timestamp versions
          // because they serve no value and just consume storage and extra time
          // if we eventually destroy this bucket.
          purgeOldVersions(timestamp_path);
      } catch (IOException e) {
          log.error("Failed to purge old versions of {}. Ignoring error.",
                    timestamp_path);
      }
  }

  // Remove the old versions of a path.
  // Versioning is per-bucket but it doesnt make sense to version the
  // timestamp objects so we'll aggressively delete those.
  private void purgeOldVersions(String path) throws IOException {
      Storage.Objects.List listObjects = obj_api.list(bucketName)
          .setPrefix(path)
          .setVersions(true);

      com.google.api.services.storage.model.Objects objects;

      // Keep the 0th object on the first page (which is current).
      List<Long> generations = new ArrayList(32);
      do {
          objects = listObjects.execute();
          List<StorageObject> items = objects.getItems();
          if (items != null) {
              int n = items.size();
              while (--n >= 0) {
                  generations.add(items.get(n).getGeneration());
              }
          }
          listObjects.setPageToken(objects.getNextPageToken());
      } while (objects.getNextPageToken() != null);

      for (long generation : generations) {
          if (generation == generations.get(0)) {
              continue;
          }
          log.debug("Remove {} generation {}", path, generation);
          obj_api.delete(bucketName, path).setGeneration(generation).execute();
      }
  }

  public long getLastModified(String daoTypeName) {
      String path = daoRoot(daoTypeName) + '/' + LAST_MODIFIED_FILENAME;
      try {
          return obj_api.get(bucketName, path).execute().getUpdated().getValue();
      } catch (HttpResponseException e) {
          if (e.getStatusCode() == 404) {
              log.error("No timestamp file at {}. Creating a new one.", path);
              writeLastModified(daoTypeName);
              return 0L;
          }
          log.error("Error writing timestamp file {}", e.toString());
          return 0L;
      } catch (Exception e) {
          log.error("Error accessing timestamp file {}", e.toString());
          return 0L;
      }
  }

  private String daoRoot(String daoTypeName) {
      return basePath + '/' + daoTypeName;
  }

  private String keyToPath(String key, String daoTypeName) {
      return daoRoot(daoTypeName) + '/' + key + '/' + dataFilename;
  }
}
