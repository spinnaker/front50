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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.BucketInfo;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.front50.model.application.Application;
import java.io.ByteArrayInputStream;
import java.util.Date;
import org.junit.Test;

public class OssStorageServiceTest {
  String bucket = "test-bucket-112121212";
  OSS oss = mock(OSS.class);
  ObjectMapper objectMapper = new ObjectMapper();
  OssStorageService storageService =
      new OssStorageService(objectMapper, oss, bucket, "/root", true, false, 1000);

  @Test
  public void test_ensureBucketExists() {
    when(oss.getBucketInfo(eq(bucket))).thenReturn(new BucketInfo());
    storageService.ensureBucketExists();
    verify(oss, times(0)).createBucket(eq(bucket));
  }

  @Test
  public void test_ensureBucketExists_noSuchBucket() {
    when(oss.getBucketInfo(eq(bucket)))
        .thenThrow(new OSSException("", "NoSuchBucket", "", null, null, null, null));
    when(oss.createBucket(eq(bucket))).thenReturn(new Bucket());
    doNothing().when(oss).setBucketVersioning(any());
    storageService.ensureBucketExists();
    verify(oss, times(1)).createBucket(eq(bucket));
    verify(oss, times(1)).setBucketVersioning(any());
  }

  @Test(expected = OSSException.class)
  public void test_ensureBucketExists_throwexception() {
    when(oss.getBucketInfo(eq(bucket)))
        .thenThrow(new OSSException("", "400", "", null, null, null, null));
    storageService.ensureBucketExists();
  }

  @Test
  public void test_loadObject() throws JsonProcessingException {
    Date curTime = new Date();

    Application application = new Application();
    application.setName("test");
    application.setDescription("desc");
    application.setCreatedAt(curTime.getTime());

    OSSObject object = new OSSObject();
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setLastModified(curTime);
    object.setObjectMetadata(metadata);
    object.setObjectContent(new ByteArrayInputStream(objectMapper.writeValueAsBytes(application)));

    when(oss.getObject(eq(bucket), anyString())).thenReturn(object);

    Application load = storageService.loadObject(ObjectType.APPLICATION, application.getName());
    assertNotNull(load);
    assertEquals(load.getLastModified().longValue(), metadata.getLastModified().getTime());
    assertEquals(load.getName(), application.getName());
    assertEquals(load.getDescription(), load.getDescription());
  }
}
