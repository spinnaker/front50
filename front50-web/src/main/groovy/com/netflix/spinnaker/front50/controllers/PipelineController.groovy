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

package com.netflix.spinnaker.front50.controllers

import com.netflix.spinnaker.front50.model.pipeline.Pipeline
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO
import groovy.transform.InheritConstructors
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * Controller for presets
 */
@RestController
@RequestMapping('pipelines')
class PipelineController {

  @Autowired
  PipelineDAO pipelineDAO

  @PreAuthorize("#restricted ? @fiatPermissionEvaluator.storeWholePermission() : true")
  @PostFilter("#restricted ? hasPermission(filterObject.name, 'APPLICATION', 'READ') : true")
  @RequestMapping(value = '', method = RequestMethod.GET)
  List<Pipeline> list(@RequestParam(required = false, value = 'restricted', defaultValue = 'true') boolean restricted) {
    pipelineDAO.all()
  }

  @PreAuthorize("hasPermission(#application, 'APPLICATION', 'READ')")
  @RequestMapping(value = '{application:.+}', method = RequestMethod.GET)
  List<Pipeline> listByApplication(@PathVariable(value = 'application') String application) {
    pipelineDAO.getPipelinesByApplication(application)
  }

  @PreAuthorize("@fiatPermissionEvaluator.storeWholePermission()")
  @PostFilter("hasPermission(filterObject.application, 'APPLICATION', 'READ')")
  @RequestMapping(value = '{id:.+}/history', method = RequestMethod.GET)
  Collection<Pipeline> getHistory(@PathVariable String id,
                                  @RequestParam(value = "limit", defaultValue = "20") int limit) {
    return pipelineDAO.history(id, limit)
  }

  @PreAuthorize("@fiatPermissionEvaluator.storeWholePermission() and hasPermission(#pipeline.application, 'APPLICATION', 'WRITE') and @authorizationSupport.hasRunAsUserPermission(#pipeline)")
  @RequestMapping(value = '', method = RequestMethod.POST)
  void save(@RequestBody Pipeline pipeline) {
    if (!pipeline.application || !pipeline.name) {
      throw new InvalidPipelineDefinition()
    }

    if (!pipeline.id) {
      checkForDuplicatePipeline(pipeline.getApplication(), pipeline.getName())
      // ensure that cron triggers are assigned a unique identifier for new pipelines
      def triggers = (pipeline.triggers ?: []) as List<Map>
      triggers.findAll { it.type == "cron" }.each { Map trigger ->
        trigger.id = UUID.randomUUID().toString()
      }
    }

    pipelineDAO.create(pipeline.id as String, pipeline)
  }

  @PreAuthorize("@fiatPermissionEvaluator.isAdmin()")
  @RequestMapping(value = 'batchUpdate', method = RequestMethod.POST)
  void batchUpdate(@RequestBody List<Pipeline> pipelines) {
    pipelineDAO.bulkImport(pipelines)
  }

  @PreAuthorize("hasPermission(#application, 'APPLICATION', 'WRITE')")
  @RequestMapping(value = '{application}/{pipeline:.+}', method = RequestMethod.DELETE)
  void delete(@PathVariable String application, @PathVariable String pipeline) {
    pipelineDAO.delete(
      pipelineDAO.getPipelineId(application, pipeline)
    )
  }

  void delete(@PathVariable String id) {
    pipelineDAO.delete(id)
  }

  @PreAuthorize("hasPermission(#command.application, 'APPLICATION', 'WRITE')")
  @RequestMapping(value = 'move', method = RequestMethod.POST)
  void rename(@RequestBody RenameCommand command) {
    checkForDuplicatePipeline(command.application, command.to)
    def pipelineId = pipelineDAO.getPipelineId(command.application, command.from)
    def pipeline = pipelineDAO.findById(pipelineId)
    pipeline.setName(command.to)

    pipelineDAO.update(pipelineId, pipeline)
  }

  static class RenameCommand {
    String application
    String from
    String to
  }

  private void checkForDuplicatePipeline(String application, String name) {
    if (pipelineDAO.getPipelinesByApplication(application).any {
      it.getName().equalsIgnoreCase(name)
    }) {
      throw new DuplicatePipelineNameException()
    }
  }

  @ExceptionHandler(DuplicatePipelineNameException)
  @ResponseStatus(BAD_REQUEST)
  Map handleDuplicatePipelineNameException() {
    return [error: "A pipeline with that name already exists in that application", status: BAD_REQUEST]
  }

  @ExceptionHandler(InvalidPipelineDefinition)
  @ResponseStatus(UNPROCESSABLE_ENTITY)
  Map handleInvalidPipelineDefinition() {
    return [error: "A pipeline requires name and application fields", status: UNPROCESSABLE_ENTITY]
  }

  @ExceptionHandler(AccessDeniedException)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  Map handleAccessDeniedException(AccessDeniedException ade) {
    return [error: "Access is denied", status: HttpStatus.FORBIDDEN.value()]
  }

  @InheritConstructors
  static class DuplicatePipelineNameException extends RuntimeException {}

  @InheritConstructors
  static class InvalidPipelineDefinition extends RuntimeException {}
}
