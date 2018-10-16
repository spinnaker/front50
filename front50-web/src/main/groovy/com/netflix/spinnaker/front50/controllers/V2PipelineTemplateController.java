/*
 * Copyright 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.netflix.spinnaker.front50.exception.BadRequestException;
import com.netflix.spinnaker.front50.exception.NotFoundException;
import com.netflix.spinnaker.front50.exceptions.DuplicateEntityException;
import com.netflix.spinnaker.front50.exceptions.InvalidRequestException;
import com.netflix.spinnaker.front50.model.pipeline.Pipeline;
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO;
import com.netflix.spinnaker.front50.model.pipeline.PipelineTemplate;
import com.netflix.spinnaker.front50.model.pipeline.PipelineTemplateDAO;
import com.netflix.spinnaker.front50.model.pipeline.TemplateConfiguration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/pipelineTemplates")
public class V2PipelineTemplateController {

  @Autowired(required = false)
  PipelineTemplateDAO pipelineTemplateDAO = null;

  @Autowired
  PipelineDAO pipelineDAO;

  @Autowired
  ObjectMapper objectMapper;

  // TODO(jacobkiefer): Add fiat authz
  // TODO(jacobkiefer): Un-stub
  @RequestMapping(value = "", method = RequestMethod.GET)
  List<PipelineTemplate> list(@RequestParam(required = false, value = "scopes") List<String> scopes) {
    return null;
  }

  @RequestMapping(value = "", method = RequestMethod.POST)
  void save(@RequestBody PipelineTemplate pipelineTemplate) {
  }

  @RequestMapping(value = "{id}", method = RequestMethod.GET)
  PipelineTemplate get(@PathVariable String id) {
    return null;
  }

  @RequestMapping(value = "{id}", method = RequestMethod.PUT)
  PipelineTemplate update(@PathVariable String id, @RequestBody PipelineTemplate pipelineTemplate) {
    return null;
  }

  @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
  void delete(@PathVariable String id) {
  }

  @RequestMapping(value = "{id}/dependentPipelines", method = RequestMethod.GET)
  List<Pipeline> listDependentPipelines(@PathVariable String id,
    @RequestParam(required = false, value = "recursive", defaultValue = "false") boolean recursive) {
    return null;
  }

  @VisibleForTesting
  List<String> getDependentConfigs(String templateId, boolean recursive) {
    return null;
  }

  @VisibleForTesting
  void checkForDependentConfigs(String templateId, boolean recursive) {
    List<String> dependentConfigIds = getDependentConfigs(templateId, recursive);
    if (dependentConfigIds.size() != 0) {
      throw new InvalidRequestException("The following pipeline configs"
        + " depend on this template: " + String.join(", ", dependentConfigIds));
    }
  }

  private void checkForDuplicatePipelineTemplate(String id) {
    try {
      getPipelineTemplateDAO().findById(id);
    } catch (NotFoundException e) {
      return;
    }
    throw new DuplicateEntityException("A pipeline template with the id " + id + " already exists");
  }

  private PipelineTemplateDAO getPipelineTemplateDAO() {
    if (pipelineTemplateDAO == null) {
      throw new BadRequestException("Pipeline Templates are not supported with your current storage backend");
    }
    return pipelineTemplateDAO;
  }
}
