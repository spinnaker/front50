package com.netflix.spinnaker.front50.model.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryEnvironment {
  String name;
  List<Map<String,Object>> infrastructure; //todo eb: let's type this.

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Map<String,Object>> getInfrastructure() {
    return infrastructure;
  }

  public void setInfrastructure(List<Map<String,Object>> infrastructure) {
    this.infrastructure = infrastructure;
  }
}
