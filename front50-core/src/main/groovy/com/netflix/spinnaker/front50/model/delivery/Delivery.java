package com.netflix.spinnaker.front50.model.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.netflix.spinnaker.front50.model.Timestamped;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Delivery implements Timestamped {
  private String id;
  private String application;
  private Long updateTs;
  private Long createTs;
  private String lastModifiedBy;
  private List<DeliveryArtifact> artifacts;
  private List<DeliveryEnvironment> environments;

  public Delivery() {
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getApplication() {
    return application;
  }

  public void setApplication(String application) {
    this.application = application;
  }

  @Override
  public Long getLastModified() {
    return updateTs;
  }

  @Override
  public void setLastModified(Long lastModified) {
    updateTs = lastModified;
  }

  @Override
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public List<DeliveryArtifact> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(List<DeliveryArtifact> artifacts) {
    this.artifacts = artifacts;
  }

  public List<DeliveryEnvironment> getEnvironments() {
    return environments;
  }

  public void setEnvironments(List<DeliveryEnvironment> environments) {
    this.environments = environments;
  }

  public Long getCreateTs() {
    return createTs;
  }

  public void setCreateTs(Long createTs) {
    this.createTs = createTs;
  }
}
