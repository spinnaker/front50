/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.front50.controllers;

import static com.netflix.spinnaker.front50.model.pipeline.Pipeline.TYPE_TEMPLATED;
import static com.netflix.spinnaker.front50.model.pipeline.TemplateConfiguration.TemplateSource.SPINNAKER_PREFIX;
import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.netflix.spinnaker.front50.ServiceAccountsService;
import com.netflix.spinnaker.front50.exception.BadRequestException;
import com.netflix.spinnaker.front50.exception.NotFoundException;
import com.netflix.spinnaker.front50.exceptions.DuplicateEntityException;
import com.netflix.spinnaker.front50.exceptions.InvalidEntityException;
import com.netflix.spinnaker.front50.exceptions.InvalidRequestException;
import com.netflix.spinnaker.front50.model.pipeline.Pipeline;
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO;
import com.netflix.spinnaker.front50.model.pipeline.PipelineTemplateDAO;
import com.netflix.spinnaker.front50.model.pipeline.TemplateConfiguration;
import com.netflix.spinnaker.front50.model.pipeline.V2TemplateConfiguration;
import com.netflix.spinnaker.front50.validator.GenericValidationErrors;
import com.netflix.spinnaker.front50.validator.PipelineValidator;
import com.netflix.spinnaker.kork.web.exceptions.ValidationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller for presets */
@RestController
@RequestMapping("pipelines")
public class PipelineController {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final PipelineDAO pipelineDAO;
  private final ObjectMapper objectMapper;
  private final Optional<ServiceAccountsService> serviceAccountsService;
  private final List<PipelineValidator> pipelineValidators;
  private final Optional<PipelineTemplateDAO> pipelineTemplateDAO;

  public PipelineController(
      PipelineDAO pipelineDAO,
      ObjectMapper objectMapper,
      Optional<ServiceAccountsService> serviceAccountsService,
      List<PipelineValidator> pipelineValidators,
      Optional<PipelineTemplateDAO> pipelineTemplateDAO) {
    this.pipelineDAO = pipelineDAO;
    this.objectMapper = objectMapper;
    this.serviceAccountsService = serviceAccountsService;
    this.pipelineValidators = pipelineValidators;
    this.pipelineTemplateDAO = pipelineTemplateDAO;
  }

  @PreAuthorize("#restricted ? @fiatPermissionEvaluator.storeWholePermission() : true")
  @PostFilter("#restricted ? hasPermission(filterObject.name, 'APPLICATION', 'READ') : true")
  @RequestMapping(value = "", method = RequestMethod.GET)
  public Collection<Pipeline> list(
      @RequestParam(required = false, value = "restricted", defaultValue = "true")
          boolean restricted,
      @RequestParam(required = false, value = "refresh", defaultValue = "true") boolean refresh) {
    return pipelineDAO.all(refresh);
  }

  @PreAuthorize("hasPermission(#application, 'APPLICATION', 'READ')")
  @RequestMapping(value = "{application:.+}", method = RequestMethod.GET)
  public List<Pipeline> listByApplication(
      @PathVariable(value = "application") String application,
      @RequestParam(required = false, value = "refresh", defaultValue = "true") boolean refresh) {
    List<Pipeline> pipelines =
        new ArrayList<>(pipelineDAO.getPipelinesByApplication(application, refresh));

    pipelines.sort(
        (p1, p2) -> {
          if (p1.getIndex() != null && p2.getIndex() == null) {
            return -1;
          }
          if (p1.getIndex() == null && p2.getIndex() != null) {
            return 1;
          }
          if (p1.getIndex() != null
              && p2.getIndex() != null
              && !p1.getIndex().equals(p2.getIndex())) {
            return p1.getIndex() - p2.getIndex();
          }
          return Optional.ofNullable(p1.getName())
              .orElse(p1.getId())
              .compareToIgnoreCase(Optional.ofNullable(p2.getName()).orElse(p2.getId()));
        });

    int i = 0;
    for (Pipeline p : pipelines) {
      p.setIndex(i);
      i++;
    }

    return pipelines;
  }

  @PreAuthorize("@fiatPermissionEvaluator.storeWholePermission()")
  @PostFilter("hasPermission(filterObject.application, 'APPLICATION', 'READ')")
  @RequestMapping(value = "{id:.+}/history", method = RequestMethod.GET)
  public Collection<Pipeline> getHistory(
      @PathVariable String id, @RequestParam(value = "limit", defaultValue = "20") int limit) {
    return pipelineDAO.history(id, limit);
  }

  @PreAuthorize("@fiatPermissionEvaluator.storeWholePermission()")
  @PostAuthorize("hasPermission(returnObject.application, 'APPLICATION', 'READ')")
  @RequestMapping(value = "{id:.+}/get", method = RequestMethod.GET)
  public Pipeline get(@PathVariable String id) {
    return pipelineDAO.findById(id);
  }

  @PreAuthorize(
      "@fiatPermissionEvaluator.storeWholePermission() and hasPermission(#pipeline.application, 'APPLICATION', 'WRITE') and @authorizationSupport.hasRunAsUserPermission(#pipeline)")
  @RequestMapping(value = "", method = RequestMethod.POST)
  public Pipeline save(@RequestBody Pipeline pipeline) {

    validatePipeline(pipeline);

    pipeline.setName(pipeline.getName().trim());
    pipeline = ensureCronTriggersHaveIdentifier(pipeline);

    if (Strings.isNullOrEmpty(pipeline.getId())
        || (boolean) pipeline.getOrDefault("regenerateCronTriggerIds", false)) {
      // ensure that cron triggers are assigned a unique identifier for new pipelines
      pipeline.getTriggers().stream()
          .filter(it -> "cron".equals(it.getType()))
          .forEach(it -> it.put("id", UUID.randomUUID().toString()));
    }

    return pipelineDAO.create(pipeline.getId(), pipeline);
  }

  @PreAuthorize("@fiatPermissionEvaluator.isAdmin()")
  @RequestMapping(value = "batchUpdate", method = RequestMethod.POST)
  public void batchUpdate(@RequestBody List<Pipeline> pipelines) {
    pipelineDAO.bulkImport(pipelines);
  }

  @PreAuthorize("hasPermission(#application, 'APPLICATION', 'WRITE')")
  @RequestMapping(value = "{application}/{pipeline:.+}", method = RequestMethod.DELETE)
  public void delete(@PathVariable String application, @PathVariable String pipeline) {
    String pipelineId = pipelineDAO.getPipelineId(application, pipeline);
    log.info(
        "Deleting pipeline \"{}\" with id {} in application {}", pipeline, pipelineId, application);
    pipelineDAO.delete(pipelineId);

    serviceAccountsService.ifPresent(
        accountsService ->
            accountsService.deleteManagedServiceAccounts(Collections.singletonList(pipelineId)));
  }

  public void delete(@PathVariable String id) {
    pipelineDAO.delete(id);
    serviceAccountsService.ifPresent(
        accountsService ->
            accountsService.deleteManagedServiceAccounts(Collections.singletonList(id)));
  }

  @PreAuthorize("hasPermission(#pipeline.application, 'APPLICATION', 'WRITE')")
  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  public Pipeline update(@PathVariable final String id, @RequestBody Pipeline pipeline) {
    Pipeline existingPipeline = pipelineDAO.findById(id);

    if (!pipeline.getId().equals(existingPipeline.getId())) {
      throw new InvalidRequestException(
          format(
              "The provided id %s doesn't match the pipeline id %s",
              pipeline.getId(), existingPipeline.getId()));
    }

    validatePipeline(pipeline);

    pipeline.setName(pipeline.getName().trim());
    pipeline.put("updateTs", System.currentTimeMillis());
    pipeline = ensureCronTriggersHaveIdentifier(pipeline);

    pipelineDAO.update(id, pipeline);

    return pipeline;
  }

  /**
   * Ensure basic validity of the pipeline. Invalid pipelines will raise runtime exceptions.
   *
   * @param pipeline The Pipeline to validate
   */
  private void validatePipeline(final Pipeline pipeline) {
    // Pipelines must have an application and a name
    if (Strings.isNullOrEmpty(pipeline.getApplication())
        || Strings.isNullOrEmpty(pipeline.getName())) {
      throw new InvalidEntityException("A pipeline requires name and application fields");
    }

    // Check if pipeline type is templated
    if (TYPE_TEMPLATED.equals(pipeline.getType())) {
      PipelineTemplateDAO templateDAO = getTemplateDAO();

      // Check templated pipelines to ensure template is valid
      String source;
      switch (pipeline.getSchema()) {
        case "v2":
          V2TemplateConfiguration v2Config =
              objectMapper.convertValue(pipeline, V2TemplateConfiguration.class);
          source = v2Config.getTemplate().getReference();
          break;
        default:
          TemplateConfiguration v1Config =
              objectMapper.convertValue(pipeline.getConfig(), TemplateConfiguration.class);
          source = v1Config.getPipeline().getTemplate().getSource();
          break;
      }

      // With the source check if it starts with "spinnaker://"
      // Check if template id which is after :// is in the store
      if (source.startsWith(SPINNAKER_PREFIX)) {
        String templateId = source.substring(SPINNAKER_PREFIX.length());
        try {
          templateDAO.findById(templateId);
        } catch (NotFoundException notFoundEx) {
          throw new BadRequestException("Configured pipeline template not found", notFoundEx);
        }
      }
    }

    checkForDuplicatePipeline(
        pipeline.getApplication(), pipeline.getName().trim(), pipeline.getId());

    final GenericValidationErrors errors = new GenericValidationErrors(pipeline);
    pipelineValidators.forEach(it -> it.validate(pipeline, errors));

    if (errors.hasErrors()) {
      List<String> message =
          errors.getAllErrors().stream()
              .map(DefaultMessageSourceResolvable::getDefaultMessage)
              .collect(Collectors.toList());
      throw new ValidationException(message);
    }
  }

  private PipelineTemplateDAO getTemplateDAO() {
    return pipelineTemplateDAO.orElseThrow(
        () ->
            new BadRequestException(
                "Pipeline Templates are not supported with your current storage backend"));
  }

  private void checkForDuplicatePipeline(String application, String name, String id) {
    boolean any =
        pipelineDAO.getPipelinesByApplication(application).stream()
            .anyMatch(it -> it.getName().equalsIgnoreCase(name) && !it.getId().equals(id));
    if (any) {
      throw new DuplicateEntityException(
          format("A pipeline with name %s already exists in application %s", name, application));
    }
  }

  private void checkForDuplicatePipeline(String application, String name) {
    checkForDuplicatePipeline(application, name, null);
  }

  private static Pipeline ensureCronTriggersHaveIdentifier(Pipeline pipeline) {
    // ensure that all cron triggers have an assigned identifier
    pipeline.getTriggers().stream()
        .filter(it -> "cron".equalsIgnoreCase(it.getType()))
        .forEach(
            it ->
                it.put(
                    "id", Optional.ofNullable(it.get("id")).orElse(UUID.randomUUID().toString())));
    return pipeline;
  }
}
