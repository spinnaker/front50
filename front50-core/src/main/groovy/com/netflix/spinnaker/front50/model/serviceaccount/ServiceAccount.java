package com.netflix.spinnaker.front50.model.serviceaccount;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.spinnaker.front50.model.Timestamped;
import java.util.ArrayList;
import java.util.List;

public class ServiceAccount implements Timestamped {

  private String name;
  private Long lastModified;
  private String lastModifiedBy;
  private List<String> memberOf = new ArrayList<>();

  @Override
  @JsonIgnore
  public String getId() {
    return name.toLowerCase();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getLastModified() {
    return lastModified;
  }

  public void setLastModified(Long lastModified) {
    this.lastModified = lastModified;
  }

  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public List<String> getMemberOf() {
    return memberOf;
  }

  public void setMemberOf(List<String> memberOf) {
    this.memberOf = memberOf;
  }
}
