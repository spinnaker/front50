package com.netflix.spinnaker.front50.config;

import java.util.concurrent.TimeUnit;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
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

  @Data
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
  }
}
