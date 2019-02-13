package com.netflix.spinnaker.front50.model.delivery;

import java.util.List;
import java.util.Map;

public class DeliveryArtifact {
  private String packageName;
  private String packageType;
  private Map bakeConfig; //todo eb: can we type this?
  private List<EnvironmentMapping> environments;

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getPackageType() {
    return packageType;
  }

  public void setPackageType(String packageType) {
    this.packageType = packageType;
  }

  public Map getBakeConfig() {
    return bakeConfig;
  }

  public void setBakeConfig(Map bakeConfig) {
    this.bakeConfig = bakeConfig;
  }

  public List<EnvironmentMapping> getEnvironments() {
    return environments;
  }

  public void setEnvironments(List<EnvironmentMapping> environments) {
    this.environments = environments;
  }
}
