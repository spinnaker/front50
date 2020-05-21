package com.netflix.spinnaker.front50.config;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage-service")
public class StorageServiceConfigurationProperties {

  private PerObjectType application = new PerObjectType(20, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType applicationPermission = new PerObjectType(20, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType serviceAccount = new PerObjectType(20, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType project = new PerObjectType(20, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType notification = new PerObjectType(20, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType pipelineStrategy = new PerObjectType(20, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType pipeline = new PerObjectType(20, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType pipelineTemplate = new PerObjectType(20, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType snapshot = new PerObjectType(2, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType deliveryConfig = new PerObjectType(20, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType pluginInfo = new PerObjectType(2, TimeUnit.MINUTES.toMillis(1));
  private PerObjectType entityTags = new PerObjectType(2, TimeUnit.MINUTES.toMillis(5), false);

  public PerObjectType getApplication() {
    return application;
  }

  public PerObjectType getApplicationPermission() {
    return applicationPermission;
  }

  public PerObjectType getServiceAccount() {
    return serviceAccount;
  }

  public PerObjectType getProject() {
    return project;
  }

  public PerObjectType getNotification() {
    return notification;
  }

  public PerObjectType getPipelineStrategy() {
    return pipelineStrategy;
  }

  public PerObjectType getPipeline() {
    return pipeline;
  }

  public PerObjectType getPipelineTemplate() {
    return pipelineTemplate;
  }

  public PerObjectType getSnapshot() {
    return snapshot;
  }

  public PerObjectType getDeliveryConfig() {
    return deliveryConfig;
  }

  public PerObjectType getPluginInfo() {
    return pluginInfo;
  }

  public PerObjectType getEntityTags() {
    return entityTags;
  }

  public static class PerObjectType {

    private int threadPool;
    private long refreshMs;
    private boolean shouldWarmCache;

    public PerObjectType(int threadPool, long refreshMs) {
      this(threadPool, refreshMs, true);
    }

    public PerObjectType(int threadPool, long refreshMs, boolean shouldWarmCache) {
      setThreadPool(threadPool);
      setRefreshMs(refreshMs);
      setShouldWarmCache(shouldWarmCache);
    }

    public void setThreadPool(int threadPool) {
      if (threadPool <= 1) {
        throw new IllegalArgumentException("threadPool must be >= 1");
      }

      this.threadPool = threadPool;
    }

    public int getThreadPool() {
      return threadPool;
    }

    public long getRefreshMs() {
      return refreshMs;
    }

    public void setRefreshMs(long refreshMs) {
      this.refreshMs = refreshMs;
    }

    public boolean isShouldWarmCache() {
      return shouldWarmCache;
    }

    public boolean getShouldWarmCache() {
      return shouldWarmCache;
    }

    public void setShouldWarmCache(boolean shouldWarmCache) {
      this.shouldWarmCache = shouldWarmCache;
    }
  }
}
