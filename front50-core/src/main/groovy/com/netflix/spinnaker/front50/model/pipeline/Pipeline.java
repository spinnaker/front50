package com.netflix.spinnaker.front50.model.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.netflix.spinnaker.front50.model.Timestamped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

public class Pipeline extends HashMap<String, Object> implements Timestamped {

  private static ObjectMapper MAPPER = new ObjectMapper();
  public static final String TYPE_TEMPLATED = "templatedPipeline";

  @JsonIgnore
  public String getApplication() {
    return (String) super.get("application");
  }

  @JsonIgnore
  public String getName() {
    return (String) super.get("name");
  }

  public void setName(String name) {
    super.put("name", name);
  }

  @Override
  @JsonIgnore
  public String getId() {
    return (String) super.get("id");
  }

  public void setId(String id) {
    super.put("id", id);
  }

  @Override
  @JsonIgnore
  public Long getLastModified() {
    String updateTs = DefaultGroovyMethods.asType(super.get("updateTs"), String.class);
    return Strings.isNullOrEmpty(updateTs) ? null : Long.valueOf(updateTs);
  }

  @Override
  public void setLastModified(Long lastModified) {
    super.put("updateTs", lastModified.toString());
  }

  @Override
  public String getLastModifiedBy() {
    return (String) super.get("lastModifiedBy");
  }

  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    super.put("lastModifiedBy", lastModifiedBy);
  }

  @JsonIgnore
  public Object getConfig() {
    return super.get("config");
  }

  @JsonIgnore
  public void setConfig(Object config) {
    super.put("config", config);
  }

  @JsonIgnore
  public String getType() {
    return (String) super.get("type");
  }

  @JsonIgnore
  public void setType(String type) {
    super.put("type", type);
  }

  @JsonIgnore
  public Collection<Trigger> getTriggers() {
    return MAPPER.convertValue(
        super.getOrDefault("triggers", new ArrayList<>()), Trigger.COLLECTION_TYPE);
  }

  public void setTriggers(Collection<Trigger> triggers) {
    this.put("triggers", triggers);
  }

  /**
   * Denotes templated pipeline config schema version.
   *
   * @return
   */
  @JsonIgnore
  public String getSchema() {
    final String get = (String) super.get("schema");
    return Strings.isNullOrEmpty(get) ? "1" : get;
  }

  @JsonIgnore
  public void setSchema(String schema) {
    super.put("schema", schema);
  }

  @JsonIgnore
  public Integer getIndex() {
    return (Integer) super.get("index");
  }

  @JsonIgnore
  public void setIndex(Integer index) {
    super.put("index", index);
  }
}
