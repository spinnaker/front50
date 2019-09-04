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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.netflix.spinnaker.fiat.model.resources.Permissions;
import com.netflix.spinnaker.fiat.model.resources.ResourceType;
import com.netflix.spinnaker.front50.model.Timestamped;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class GroupPermission implements Timestamped {
  private @Setter @Getter Permissions permissions = Permissions.EMPTY;
  private @Setter @Getter String resourceGroupType;
  private @Setter @Getter ResourceType resourceType;
  private @Setter @Getter Long lastModified;
  private @Setter @Getter String lastModifiedBy;
  private @Setter @Getter String id;

  // Fields filled depending on the group type
  private Map<String, Object> details = new HashMap<>();

  @JsonAnyGetter
  public Map<String, Object> getDetails() {
    return details;
  }

  @JsonAnySetter
  public void setDetails(String name, Object value) {
    details.put(name, value);
  }
}
