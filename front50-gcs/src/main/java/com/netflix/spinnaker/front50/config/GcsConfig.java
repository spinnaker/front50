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

package com.netflix.spinnaker.front50.config;

import com.netflix.spinnaker.front50.model.*;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.schedulers.Schedulers;

import java.io.FileInputStream;
import java.util.concurrent.Executors;
import java.util.Collections;

@Configuration
public class GcsConfig {
  @Value("${spinnaker.gcs.bucket}")
  private String bucket;

  @Value("${spinnaker.gcs.rootFolder}")
  private String rootFolder;

  @Value("${providers.google.primaryCredentials.jsonPath}")
  private String jsonPath;

  @Value("${Implementation-Version:Unknown}")
  private String applicationVersion;

  @Bean
  public Storage googleCloudStorageClient() {
      try {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        GoogleCredential credential;
        Logger log = LoggerFactory.getLogger(getClass());

        if (!jsonPath.isEmpty()) {
            FileInputStream credentialStream = new FileInputStream(jsonPath);
            credential = GoogleCredential.fromStream(credentialStream, httpTransport, jsonFactory)
                                         .createScoped(Collections.singleton(StorageScopes.DEVSTORAGE_FULL_CONTROL));
          log.info("Loaded credentials from from " + jsonPath);
        } else {
            log.info("spinnaker.gcs.enabled without providers.google.primaryCredentials.jsonPath. Using default application credentials. Using default credentials.");
            credential = GoogleCredential.getApplicationDefault();
        }

        String applicationName = "Spinnaker-front50/" + applicationVersion;
        return new Storage.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .build();
      } catch (java.io.IOException|java.security.GeneralSecurityException e) {
          throw new IllegalStateException(e);
      }
  }

  @Bean
  @ConditionalOnMissingBean(RestTemplate.class)
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public ObjectMapper gcsObjectMapper() {
    return new ObjectMapper();
  }

  @Bean
  @ConditionalOnExpression("${spinnaker.gcs.enabled:false}")
  public GcsApplicationDAO gcsApplicationDAO(ObjectMapper objectMapper, Storage storage) {
    return new GcsApplicationDAO(objectMapper, storage, Schedulers.from(Executors.newFixedThreadPool(20)), 15000, bucket, rootFolder);
  }

  @Bean
  @ConditionalOnExpression("${spinnaker.gcs.enabled:false}")
  public GcsProjectDAO gcsProjectDAO(ObjectMapper objectMapper, Storage storage) {
    return new GcsProjectDAO(objectMapper, storage, Schedulers.from(Executors.newFixedThreadPool(10)), 30000, bucket, rootFolder);
  }

  @Bean
  @ConditionalOnExpression("${spinnaker.gcs.enabled:false}")
  public GcsNotificationDAO gcsNotificationDAO(ObjectMapper objectMapper, Storage storage) {
    return new GcsNotificationDAO(objectMapper, storage, Schedulers.from(Executors.newFixedThreadPool(5)), 30000, bucket, rootFolder);
  }

  @Bean
  @ConditionalOnExpression("${spinnaker.gcs.enabled:false}")
  public GcsPipelineStrategyDAO gcsPipelineStrategyDAO(ObjectMapper objectMapper, Storage storage) {
    return new GcsPipelineStrategyDAO(objectMapper, storage, Schedulers.from(Executors.newFixedThreadPool(5)), 20000, bucket, rootFolder);
  }

  @Bean
  @ConditionalOnExpression("${spinnaker.gcs.enabled:false}")
  public GcsPipelineDAO gcsPipelineDAO(ObjectMapper objectMapper, Storage storage) {
    return new GcsPipelineDAO(objectMapper, storage, Schedulers.from(Executors.newFixedThreadPool(25)), 10000, bucket, rootFolder);
  }
}
