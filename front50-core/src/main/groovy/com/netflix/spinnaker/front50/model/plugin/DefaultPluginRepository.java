/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.front50.model.plugin;

import com.google.common.hash.Hashing;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.front50.exception.NotFoundException;
import com.netflix.spinnaker.front50.model.ObjectKeyLoader;
import com.netflix.spinnaker.front50.model.ObjectType;
import com.netflix.spinnaker.front50.model.StorageService;
import com.netflix.spinnaker.front50.model.StorageServiceSupport;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import rx.Scheduler;

public class DefaultPluginRepository extends StorageServiceSupport<Plugin>
    implements PluginRepository {

  public DefaultPluginRepository(
      StorageService service,
      Scheduler scheduler,
      ObjectKeyLoader objectKeyLoader,
      long refreshIntervalMs,
      boolean shouldWarmCache,
      Registry registry) {
    super(
        ObjectType.PLUGIN,
        service,
        scheduler,
        objectKeyLoader,
        refreshIntervalMs,
        shouldWarmCache,
        registry);
  }

  @Override
  public Collection<Plugin> list(boolean enabled) {
    return all().stream()
        .filter(plugin -> plugin.isEnabled() == enabled)
        .collect(Collectors.toList());
  }

  @Override
  public Plugin get(String namespace, String name) throws NotFoundException {
    String id = Hashing.sha256().hashString(namespace + name, StandardCharsets.UTF_8).toString();
    return all().stream()
        .filter(plugin -> plugin.getId().equalsIgnoreCase(id))
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format(
                        "No plugin found with namespace of %s and name of %s", namespace, name)));
  }

  @Override
  public Plugin upsert(Plugin plugin) {
    return create(plugin.getId(), plugin);
  }

  @Override
  public Plugin create(String id, Plugin plugin) {
    plugin.setLastModified(System.currentTimeMillis());

    try {
      Objects.requireNonNull(plugin.getName(), "Plugin name must NOT be null!");
      Objects.requireNonNull(plugin.getNamespace(), "Plugin namespace must NOT be null!");
      Objects.requireNonNull(plugin.getDescription(), "Plugin description must NOT be null!");
      Objects.requireNonNull(plugin.getAuthor(), "Plugin author must NOT be null!");
      Objects.requireNonNull(plugin.getVersion(), "Plugin version must NOT be null!");
      Objects.requireNonNull(plugin.getDependencies(), "Plugin dependencies must NOT be null!");
      Objects.requireNonNull(
          plugin.getRequiredVersions(), "Plugin requiredVersions must NOT be null!");
      Objects.requireNonNull(plugin.getCapabilities(), "Plugin capabilities must NOT be null!");
    } catch (NullPointerException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    update(id, plugin);
    return findById(id);
  }
}
