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
 *
 */

package com.netflix.spinnaker.front50.controllers;

import static com.netflix.spinnaker.front50.model.pipeline.Pipeline.TYPE_TEMPLATED;
import static com.netflix.spinnaker.front50.model.pipeline.TemplateConfiguration.TemplateSource.SPINNAKER_PREFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/pipelineTemplates")
@Slf4j
public class V2PipelineTemplateController {

  // TODO(jacobkiefer): Does this handle all the version strings we want to support?
  private static final String VALID_TEMPLATE_VERSION_REGEX = "^[a-zA-Z0-9-_.]+$";

  @Autowired(required = false)
  PipelineTemplateDAO pipelineTemplateDAO = null;

  @Autowired
  PipelineDAO pipelineDAO;

  @Autowired
  ObjectMapper objectMapper;

  // TODO(jacobkiefer): Add fiat authz
  @RequestMapping(value = "", method = RequestMethod.GET)
  List<PipelineTemplate> list(@RequestParam(required = false, value = "scopes") List<String> scopes) {
    return (List<PipelineTemplate>) getPipelineTemplateDAO().getPipelineTemplatesByScope(scopes);
  }

  @RequestMapping(value = "", method = RequestMethod.POST)
  void save(@RequestParam(value = "version", required = false) String version, @RequestBody PipelineTemplate pipelineTemplate) {
    if (StringUtils.isNotEmpty(version)) {
      validatePipelineTemplateVersion(version);
    }

    // NOTE: We need to store the version in the template blob to resolve the proper id later.
    String templateId;
    if (StringUtils.isNotEmpty(version)) {
      templateId = String.format("%s:%s", pipelineTemplate.undecoratedId(), version);
      pipelineTemplate.setVersion(version);
    } else {
      templateId = pipelineTemplate.undecoratedId();
    }

    try {
      checkForDuplicatePipelineTemplate(templateId);
    } catch (DuplicateEntityException dee) {
      log.debug("Updating latest version for template with id: {}", templateId);
      return;
    }
    getPipelineTemplateDAO().create(templateId, pipelineTemplate);
    saveDigest(pipelineTemplate);
  }

  public void saveDigest(PipelineTemplate pipelineTemplate) {
    pipelineTemplate.remove("digest");
    String digest = computeSHA256Digest(pipelineTemplate);
    String digestId = String.format("%s@sha256:%s", pipelineTemplate.undecoratedId(), digest);
    pipelineTemplate.setDigest(digest);
    try {
      checkForDuplicatePipelineTemplate(digestId);
    } catch (DuplicateEntityException dee) {
      log.debug("Duplicate pipeline digest calculated, not updating key {}", digestId);
      return;
    }
    getPipelineTemplateDAO().create(digestId, pipelineTemplate);
  }

  @RequestMapping(value = "{id}", method = RequestMethod.PUT)
  PipelineTemplate update(@PathVariable String id, @RequestParam(value = "version", required = false) String version, @RequestBody PipelineTemplate pipelineTemplate) {
    if (StringUtils.isNotEmpty(version)) {
      validatePipelineTemplateVersion(version);
    }

    String templateId = StringUtils.isNotEmpty(version) ? String.format("%s:%s", id, version) : pipelineTemplate.undecoratedId();
    pipelineTemplate.setLastModified(System.currentTimeMillis());
    getPipelineTemplateDAO().update(templateId, pipelineTemplate);
    updateDigest(pipelineTemplate);
    return pipelineTemplate;
  }

  private void updateDigest(PipelineTemplate pipelineTemplate) {
    String digestId = String.format("%s@sha256:%s", pipelineTemplate.undecoratedId(), computeSHA256Digest(pipelineTemplate));
    getPipelineTemplateDAO().update(digestId, pipelineTemplate);
  }

  @RequestMapping(value = "{id}", method = RequestMethod.GET)
  PipelineTemplate get(@PathVariable String id,
                       @RequestParam(value = "version", required = false) String version,
                       @RequestParam(value = "digest", required = false) String digest) {
    String templateId = formatId(id, version, digest);
    // We don't need to surface our internal accounting information to the user.
    // This would muddle the API and probably be bug-friendly.
    PipelineTemplate foundTemplate = getPipelineTemplateDAO().findById(templateId);
    foundTemplate.remove("digest");
    foundTemplate.remove("version");

    return foundTemplate;
  }

  @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
  void delete(@PathVariable String id,
              @RequestParam(value = "version", required = false) String version,
              @RequestParam(value = "digest", required = false) String digest) {
    String templateId = formatId(id, version, digest);
    // TODO(jacobkiefer): Refactor dependent config checking once we replace templateSource with Artifact(s).
    checkForDependentConfigs(templateId);
    getPipelineTemplateDAO().delete(templateId);
  }

  @RequestMapping(value = "{id}/dependentPipelines", method = RequestMethod.GET)
  List<Pipeline> listDependentPipelines(@PathVariable String id) {
    List<String> dependentConfigsIds = getDependentConfigs(id);

    return pipelineDAO.all()
      .stream()
      .filter(pipeline -> dependentConfigsIds.contains(pipeline.getId()))
      .collect(Collectors.toList());
  }

  @VisibleForTesting
  List<String> getDependentConfigs(String templateId) {
    List<String> dependentConfigIds = new ArrayList<>();

    String prefixedId = SPINNAKER_PREFIX + templateId;
    pipelineDAO.all()
      .stream()
      .filter(pipeline -> pipeline.getType() != null && pipeline.getType().equals(TYPE_TEMPLATED))
      .forEach(templatedPipeline -> {
        String source;
        try {
          TemplateConfiguration config =
            objectMapper.convertValue(templatedPipeline.getConfig(), TemplateConfiguration.class);

          source = config.getPipeline().getTemplate().getSource();
        } catch (Exception e) {
          return;
        }

        if (source != null && source.equalsIgnoreCase(prefixedId)) {
          dependentConfigIds.add(templatedPipeline.getId());
        }
      });
    return dependentConfigIds;
  }

  @VisibleForTesting
  void checkForDependentConfigs(String templateId) {
    List<String> dependentConfigIds = getDependentConfigs(templateId);
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

  @VisibleForTesting
  public String computeSHA256Digest(PipelineTemplate pipelineTemplate) {
    Map<String, Object> sortedMap = (Map<String, Object>) sortObjectRecursive(pipelineTemplate);
    try {
      String jsonPayload = objectMapper.writeValueAsString(sortedMap).replaceAll("\\s+", "");
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(jsonPayload.getBytes(StandardCharsets.UTF_8));
      return Hex.encodeHexString(hashBytes);
    } catch (NoSuchAlgorithmException | JsonProcessingException e) {
      throw new InvalidRequestException(String.format("Computing digest for pipeline template %s failed. Nested exception is %s", pipelineTemplate.undecoratedId(), e));
    }
  }

  private Object sortObjectRecursive(Object initialObj) {
    if (initialObj instanceof Map) {
      Map<String, Object> initialMap = (Map<String, Object>) initialObj;
      TreeMap<String, Object> sortedMap = new TreeMap<>();
      initialMap.forEach((k, v) -> sortedMap.put(k, sortObjectRecursive(v)));
      return sortedMap;
    } else if (initialObj instanceof List) {
      List initialList = (List) initialObj;
      return initialList.stream().map(this::sortObjectRecursive).collect(Collectors.toList());
    }  else {
      return initialObj;
    }
  }

  private PipelineTemplateDAO getPipelineTemplateDAO() {
    if (pipelineTemplateDAO == null) {
      throw new BadRequestException("Pipeline Templates are not supported with your current storage backend");
    }
    return pipelineTemplateDAO;
  }

  private void validatePipelineTemplateVersion(String version) {
    if (!version.matches(VALID_TEMPLATE_VERSION_REGEX)) {
      throw new InvalidRequestException(String.format("The provided version %s contains illegal characters."
        + " Pipeline template versions must match %s", version, VALID_TEMPLATE_VERSION_REGEX));
    }
  }

  private String formatId(String id, String version, String digest) {
    if (StringUtils.isNotEmpty(digest) && StringUtils.isNotEmpty(version)) {
      throw new InvalidRequestException("Cannot query pipeline by 'version' and 'digest' simultaneously. Specify one of 'version' or 'digest'.");
    }

    if (StringUtils.isNotEmpty(digest)) {
      return String.format("%s@sha256:%s", id, digest);
    } else if (StringUtils.isNotEmpty(version)) {
      return String.format("%s:%s", id, version);
    } else {
      return id;
    }
  }
}
