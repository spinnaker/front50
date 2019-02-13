package com.netflix.spinnaker.front50.model.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentMapping {
  private String name;
  private DeployWith deployWith;
  private List<String> constraints;
  private int order;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DeployWith getDeployWith() {
    return deployWith;
  }

  public void setDeployWith(DeployWith deployWith) {
    this.deployWith = deployWith;
  }

  public List<String> getConstraints() {
    return constraints;
  }

  public void setConstraints(List<String> constraints) {
    this.constraints = constraints;
  }

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public static class DeployWith {
    private String pipelineId;
    private Map<String, Object> pipelineConfig;

    public String getPipelineId() {
      return pipelineId;
    }

    public void setPipelineId(String pipelineId) {
      this.pipelineId = pipelineId;
    }

    public Map<String, Object> getPipelineConfig() {
      return pipelineConfig;
    }

    public void setPipelineConfig(Map<String, Object> pipelineConfig) {
      this.pipelineConfig = pipelineConfig;
    }
  }
}
