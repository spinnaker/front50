package com.netflix.spinnaker.front50.controllers;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.netflix.spinnaker.front50.model.plugin.PluginRepository;
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.GetCriteria;
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.ListCriteria;
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.UpsertData;
import com.netflix.spinnaker.front50.proto.Plugin;
import com.netflix.spinnaker.front50.proto.Plugins;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import com.netflix.spinnaker.security.AuthenticatedRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "plugins")
@ConditionalOnExpression("${spinnaker.plugins.enabled:false}")
public class PluginController {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private PluginRepository<Plugin> pluginRepository;

  public PluginController(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  @RequestMapping(method = RequestMethod.POST)
  public void upsert(@RequestBody Plugin plugin) {
    String modifiedBy = AuthenticatedRequest.getSpinnakerUser().orElse("anonymous");
    UpsertData<Plugin> upsertData = new UpsertData<>(plugin, modifiedBy);
    pluginRepository.upsert(upsertData);
  }

  @RequestMapping(method = RequestMethod.GET)
  public Plugins list(
      @RequestParam(required = false, value = "enabled", defaultValue = "true") boolean enabled) {
    ListCriteria listCriteria = new ListCriteria(enabled);
    return Plugins.newBuilder().addAllPlugin(pluginRepository.list(listCriteria)).build();
  }

  @RequestMapping(value = "{namespace}/{name}", method = RequestMethod.GET)
  public Plugin get(@PathVariable String namespace, @PathVariable String name) {
    GetCriteria getCriteria = new GetCriteria(namespace, name);
    return pluginRepository.get(getCriteria);
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(NOT_FOUND)
  void onNotFound(@NotNull NotFoundException e) {
    log.error(e.getMessage());
  }

  @ExceptionHandler(HttpMessageConversionException.class)
  @ResponseStatus(BAD_REQUEST)
  void onParseFailure(HttpMessageConversionException e) {
    log.error(e.getMessage());
  }
}
