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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spinnaker.oss")
public class OssProperties {
  private String endpoint;
  private String bucket;
  private String accessKeyId;
  private String accessSecretKey;
  private Integer maxKeys = 1000;
  private String rootFolder = "front50";
  private Boolean versioning;
  private Boolean readOnlyMode;

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public void setAccessKeyId(String accessKeyId) {
    this.accessKeyId = accessKeyId;
  }

  @JsonIgnore
  public String getAccessSecretKey() {
    return accessSecretKey;
  }

  public void setAccessSecretKey(String accessSecretKey) {
    this.accessSecretKey = accessSecretKey;
  }

  public Integer getMaxKeys() {
    return maxKeys;
  }

  public void setMaxKeys(Integer maxKeys) {
    this.maxKeys = maxKeys;
  }

  public String getRootFolder() {
    return rootFolder;
  }

  public void setRootFolder(String rootFolder) {
    this.rootFolder = rootFolder;
  }

  public Boolean getVersioning() {
    return versioning;
  }

  public void setVersioning(Boolean versioning) {
    this.versioning = versioning;
  }

  public Boolean getReadOnlyMode() {
    return readOnlyMode;
  }

  public void setReadOnlyMode(Boolean readOnlyMode) {
    this.readOnlyMode = readOnlyMode;
  }
}
