/*
 * Copyright 2021 Netflix, Inc.
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
 *
 */
package com.netflix.spinnaker.front50.api.model.pipeline;

import com.netflix.spinnaker.front50.api.model.Timestamped;

import java.util.*;

public class Pipeline implements Timestamped {

  public static final String TYPE_TEMPLATED = "templatedPipeline";

  private Map<String, Object> anyMap = new HashMap<>();
  private String id;
  private String name;
  private String application;
  private String email;
  private String type;
  private String updateTs;
  private String createTs;
  private String lastModifiedBy;
  private String schema;
  private Object config;
  private List<Trigger> triggers = new ArrayList<Trigger>();
  private Integer index;

  private List<String> inherit;
  private List<String> exclude;

  private Map<String, Object> template;
  private List<String> roles;
  private String runAsUser;
  private String serviceAccount;
  private List<Map<String, Object>> stages;
  private List<Map<String, Object>> expectedArtifacts;
  private Boolean parallel;
  private Map<String, Object> constraints;
  private Map<String, Object> payloadConstraints;

  public void setAny(String key, Object value) {
    anyMap.put(key, value);
  };

  public Map<String, Object> getAny() {
    return anyMap;
  };

  public String getApplication() {
    return this.application;
  }

  public void setApplication(String application) {
    this.application = application;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return this.email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public Long getLastModified() {
    String updateTs = this.updateTs;
    if (updateTs == null || updateTs == "") {
      return null;
    }
    return Long.valueOf(updateTs);
  }

  @Override
  public void setLastModified(Long lastModified) {
    if (lastModified != null) {
      this.updateTs = lastModified.toString();
    }
  }

  @Override
  public String getLastModifiedBy() {
    return this.lastModifiedBy;
  }

  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public Object getConfig() {
    return this.config;
  }

  public void setConfig(Object config) {
    this.config = config;
  }

  public String getType() {
    return this.type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<Trigger> getTriggers() {
    return this.triggers;
  }

  public void setTriggers(List<Trigger> triggers) {
    this.triggers = triggers;
  }

  /**
   * Denotes templated pipeline config schema version.
   *
   * @return
   */
  public String getSchema() {
    final String get = this.schema;
    if (get == null || get == "") {
      return "1";
    }
    return get;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public Integer getIndex() {
    return this.index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public List<String> getInherit() {
    return this.inherit;
  }

  public void setInherit(List<String> inherit) {
    this.inherit = inherit;
  }

  public void removeInherit() {
    this.inherit = new ArrayList();
  }

  public List<String> getExclude() {
    return this.exclude;
  }

  public void setExclude(List<String> exclude) {
    this.exclude = exclude;
  }

  public Map<String, Object> getTemplate() {
    return this.template;
  }

  public void setTemplate(Map<String, Object> template) {
    this.template = template;
  }

  public List<String> getRoles() {
    return this.roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public String getRunAsUser() {
    return this.runAsUser;
  }

  public void setRoles(String runAsUser) {
    this.runAsUser = runAsUser;
  }

  public String getServiceAccount() {
    return this.serviceAccount;
  }

  public void setServiceAccount(String serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public List<Map<String, Object>> getStages() {
    return this.stages;
  }

  public void setStages(List<Map<String, Object>> stages) {
    this.stages = stages;
  }

  public List<Map<String, Object>> getExpectedArtifacts() {
    return this.expectedArtifacts;
  }

  public void setExpectedArtifacts(List<Map<String, Object>> expectedArtifacts) {
    this.expectedArtifacts = expectedArtifacts;
  }

  public Boolean getParallel() {
    return this.parallel;
  }

  public void setParallel(Boolean parallel) {
    this.parallel = parallel;
  }

  public Map<String, Object> getConstraints() {
    return this.constraints;
  }

  public void setConstraints(Map<String, Object> constraints) {
    this.constraints = constraints;
  }

  public Map<String, Object> getPayloadConstraints() {
    return this.payloadConstraints;
  }

  public void setPayloadConstraints(Map<String, Object> payloadConstraints) {
    this.payloadConstraints = payloadConstraints;
  }
}
