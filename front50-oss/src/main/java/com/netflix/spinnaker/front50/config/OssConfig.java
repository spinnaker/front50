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

package com.netflix.spinnaker.front50.config;

import com.aliyun.oss.OSS;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.front50.model.OssStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("spinnaker.oss.enabled")
@EnableConfigurationProperties(OssProperties.class)
public class OssConfig {
  @Bean
  public OSS ossClient(OssProperties ossProperties) {
    return OssClientFactory.client(ossProperties);
  }

  @Bean
  public OssStorageService ossStorageService(OSS oss, OssProperties ossProperties) {
    OssStorageService storageService =
        new OssStorageService(
            new ObjectMapper(),
            oss,
            ossProperties.getBucket(),
            ossProperties.getRootFolder(),
            ossProperties.getVersioning(),
            ossProperties.getReadOnlyMode(),
            ossProperties.getMaxKeys());
    storageService.ensureBucketExists();
    return storageService;
  }
}
