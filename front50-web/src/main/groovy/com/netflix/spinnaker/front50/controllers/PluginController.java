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

package com.netflix.spinnaker.front50.controllers;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.netflix.spinnaker.front50.exception.BadRequestException;
import com.netflix.spinnaker.front50.exception.NotFoundException;
import com.netflix.spinnaker.front50.model.plugin.Plugin;
import com.netflix.spinnaker.front50.model.plugin.PluginRepository;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "plugins")
@ConditionalOnExpression("${spinnaker.plugins.enabled:false}")
public class PluginController {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private PluginRepository pluginRepository;

  public PluginController(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  @RequestMapping(method = RequestMethod.POST)
  public Plugin create(@RequestBody Plugin plugin) {
    return upsert(plugin);
  }

  @RequestMapping(value = "{namespace}/{name}", method = RequestMethod.PUT)
  public Plugin update(
      @PathVariable String namespace, @PathVariable String name, @RequestBody Plugin plugin) {
    if (pluginRepository.get(namespace, name) != null) {
      return upsert(plugin);
    }
    // this should never happen, since pluginRepository.get(namespace, name) throws
    // NotFoundException
    return null;
  }

  @RequestMapping(method = RequestMethod.GET)
  public Collection<Plugin> list(
      @RequestParam(required = false, value = "enabled", defaultValue = "true") boolean enabled) {
    return pluginRepository.list(enabled);
  }

  @RequestMapping(value = "{namespace}/{name}", method = RequestMethod.GET)
  public Plugin get(@PathVariable String namespace, @PathVariable String name) {
    return pluginRepository.get(namespace, name);
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(NOT_FOUND)
  void onNotFound(NotFoundException e) {
    log.error(e.getMessage());
  }

  private Plugin upsert(Plugin plugin) {
    try {
      return pluginRepository.upsert(plugin);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }
}
