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

package com.netflix.spinnaker.front50.model.grouppermission;

import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.fiat.model.resources.ResourceType;
import com.netflix.spinnaker.front50.model.ObjectKeyLoader;
import com.netflix.spinnaker.front50.model.ObjectType;
import com.netflix.spinnaker.front50.model.StorageService;
import com.netflix.spinnaker.front50.model.StorageServiceSupport;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import rx.Scheduler;

public class DefaultGroupPermissionDAO extends StorageServiceSupport<GroupPermission>
    implements GroupPermissionDAO {

  public DefaultGroupPermissionDAO(
      StorageService service,
      Scheduler scheduler,
      ObjectKeyLoader objectKeyLoader,
      long refreshIntervalMs,
      boolean shouldWarmCache,
      Registry registry) {
    super(
        ObjectType.GROUP_PERMISSION,
        service,
        scheduler,
        objectKeyLoader,
        refreshIntervalMs,
        shouldWarmCache,
        registry);
  }

  @Override
  public GroupPermission create(String id, GroupPermission groupPermission) {
    update(id, groupPermission);
    return findById(id);
  }

  @Override
  public void update(String id, GroupPermission groupPermission) {
    if (id == null) {
      id = UUID.randomUUID().toString();
    }
    groupPermission.setId(id);

    super.update(id, groupPermission);
  }

  @Override
  public Collection<GroupPermission> findAllByResourceType(ResourceType resourceType) {
    return all().stream()
        .filter(item -> item.getResourceType() == resourceType)
        .collect(Collectors.toSet());
  }
}
