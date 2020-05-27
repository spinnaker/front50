package com.netflix.spinnaker.front50.model.snapshot;

import com.netflix.spinnaker.front50.model.Timestamped;
import java.util.Map;

public class Snapshot implements Timestamped {

  private String id;
  private String application;
  private String account;
  private Map infrastructure;
  private Type configLang;
  private Long lastModified;
  private String lastModifiedBy;
  private Long timestamp;

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

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public Map getInfrastructure() {
    return infrastructure;
  }

  public void setInfrastructure(Map infrastructure) {
    this.infrastructure = infrastructure;
  }

  public Type getConfigLang() {
    return configLang;
  }

  public void setConfigLang(Type configLang) {
    this.configLang = configLang;
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

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  // TODO(rz): Why is this an enum? It isn't even used.
  public enum Type {
    TERRAFORM;
  }
}
