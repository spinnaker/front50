package com.netflix.spinnaker.front50.model.plugin;

import java.util.List;

public interface PluginRepository<T> {
  /**
   * Inserts or updates the specified plugin.
   *
   * @param data The plugin data to store
   */
  void upsert(UpsertData<T> data);

  /**
   * Returns the plugin by the specified name.
   *
   * @param criteria The criteria by which to get the plugin.
   * @return The plugin object.
   */
  T get(GetCriteria criteria);

  /**
   * Get all plugins based on the criteria.
   *
   * @param criteria The criteria by which to filter plugins by.
   * @return A list of matching plugins.
   */
  List<T> list(ListCriteria criteria);

  class GetCriteria {
    public GetCriteria(String namespace, String name) {
      this.namespace = namespace;
      this.name = name;
    }

    public String getNamespace() {
      return namespace;
    }

    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    private String namespace;
    private String name;
  }

  class ListCriteria {
    public ListCriteria(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    private boolean enabled;
  }

  class UpsertData<T> {
    public UpsertData(T plugin, String modifiedBy) {
      this.plugin = plugin;
      this.modifiedBy = modifiedBy;
    }

    public T getPlugin() {
      return plugin;
    }

    public void setPlugin(T plugin) {
      this.plugin = plugin;
    }

    public String getModifiedBy() {
      return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
      this.modifiedBy = modifiedBy;
    }

    private T plugin;
    private String modifiedBy;
  }
}
