/*
 * Copyright 2019 Netflix, Inc.
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

package com.netflix.spinnaker.front50.model.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.hash.Hashing;
import com.netflix.spinnaker.front50.model.Timestamped;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Plugin implements Timestamped {
  private String name;
  private String namespace;
  private String description;
  private String author;
  private String version;
  private List<String> dependencies;
  private List<String> requiredVersions;
  private List<String> capabilities;
  private boolean enabled;
  private boolean inProcessUnsafe;

  private Long lastModified;
  private String lastModifiedBy;

  @Override
  @JsonIgnore
  public String getId() {
    return Hashing.sha256().hashString(namespace + name, StandardCharsets.UTF_8).toString();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<String> dependencies) {
    this.dependencies = dependencies;
  }

  public List<String> getRequiredVersions() {
    return requiredVersions;
  }

  public void setRequiredVersions(List<String> requiredVersions) {
    this.requiredVersions = requiredVersions;
  }

  public List<String> getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(List<String> capabilities) {
    this.capabilities = capabilities;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isInProcessUnsafe() {
    return inProcessUnsafe;
  }

  public void setInProcessUnsafe(boolean inProcessUnsafe) {
    this.inProcessUnsafe = inProcessUnsafe;
  }

  @Override
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  @Override
  public Long getLastModified() {
    return lastModified;
  }

  @Override
  public void setLastModified(Long lastModified) {
    this.lastModified = lastModified;
  }
}
