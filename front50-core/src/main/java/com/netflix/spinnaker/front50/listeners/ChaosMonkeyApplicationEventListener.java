/*
 * Copyright 2020 Netflix, Inc.
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
package com.netflix.spinnaker.front50.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.front50.ApplicationPermissionsService;
import com.netflix.spinnaker.front50.config.ChaosMonkeyEventListenerConfigurationProperties;
import com.netflix.spinnaker.front50.events.ApplicationEventListener;
import com.netflix.spinnaker.front50.model.application.Application;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ensures that when Chaos Monkey is enabled (or disabled) on an Application, its permissions are
 * applied correctly.
 *
 * <p>This listens on both Application update events.
 */
@Component
public class ChaosMonkeyApplicationEventListener extends ChaosMonkeyEventListener
    implements ApplicationEventListener {
  private static final Logger log =
      LoggerFactory.getLogger(ChaosMonkeyApplicationEventListener.class);

  private final ApplicationPermissionsService applicationPermissionsService;

  public ChaosMonkeyApplicationEventListener(
      ApplicationPermissionsService applicationPermissionsService,
      ChaosMonkeyEventListenerConfigurationProperties properties,
      ObjectMapper objectMapper) {
    super(properties, objectMapper);
    this.applicationPermissionsService = applicationPermissionsService;
  }

  @Override
  public boolean supports(ApplicationEventListener.Type type) {
    return properties.isEnabled() && ApplicationEventListener.Type.PRE_UPDATE == type;
  }

  @Override
  public void accept(ApplicationModelEvent event) {

    Application.Permission permission;
    try {
      permission =
          applicationPermissionsService.getApplicationPermission(event.getApplication().getName());
      if (!permission.getPermissions().isRestricted()) {
        return;
      }
    } catch (NotFoundException e) {
      // This usually happens if the application permission record is deleted or permissions weren't
      // set for the app yet.
      log.warn(
          "Permission record not found for application: {}. Chaos Monkey permissions won't be applied now.",
          event.getApplication().getName(),
          e);
      return;
    }

    applyNewPermissions(permission, isChaosMonkeyEnabled(event.getApplication()));

    Application.Permission updatedPermission =
        applicationPermissionsService.updateApplicationPermission(
            event.getApplication().getName(), permission, true);

    log.debug(
        "Updated application `{}` with permissions `{}`",
        event.getApplication().getName(),
        updatedPermission.getPermissions().toString());
  }
}
