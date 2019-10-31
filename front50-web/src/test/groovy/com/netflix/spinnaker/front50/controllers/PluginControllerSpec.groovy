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

package com.netflix.spinnaker.front50.controllers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.front50.exception.NotFoundException
import com.netflix.spinnaker.front50.model.plugin.Plugin
import com.netflix.spinnaker.front50.model.plugin.PluginRepository
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification
import spock.lang.Subject

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class PluginControllerSpec extends Specification {

  Plugin plugin

  PluginRepository pluginRepository

  MockMvc mockMvc

  ObjectMapper objectMapper

  @Subject
  PluginController controller

  void setup() {
    plugin = new Plugin(
      name: "plugin",
      namespace: "netflix",
      description: "A plugin",
      author: "netflix",
      version: "1.0.0",
      capabilities: ["orcaStage"],
      dependencies: ["a dep", "another dep"],
      requiredVersions: ["orca>=1", "clouddriver>=1"],
      enabled: true,
      inProcessUnsafe: false
    )

    objectMapper = new ObjectMapper()

    pluginRepository = Mock(PluginRepository)

    controller = new PluginController(pluginRepository)

    mockMvc = MockMvcBuilders
      .standaloneSetup(controller)
      .setControllerAdvice(controller)
      .build()
  }

  def "post plugin"() {
    when:
    def response = mockMvc
      .perform(post("/plugins")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(plugin)))
      .andExpect(status().isOk())
      .andReturn()
      .response

    then:
    assert response.status == 200
  }

  def "get plugin"() {
    given:
    pluginRepository.get(_, _) >> plugin

    when:
    def response = mockMvc
      .perform(get("/plugins/namespace/name")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn()
      .response
      .contentAsByteArray

    Plugin result = objectMapper.readValue(response, Plugin.class)

    then:
    assert result.name == plugin.name
    assert result.namespace == plugin.namespace
  }

  def "get a plugin that doesn't exist"() {
    given:
    pluginRepository.get(_, _) >> { throw new NotFoundException("Not Found") }

    when:
    def response = mockMvc
      .perform(get("/plugins/namespace/name")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().is4xxClientError())
      .andReturn()
      .response

    then:
    assert response.status == 404
  }

  def "mvcmock - list plugins"() {
    given:
    pluginRepository.list(_) >> [plugin]

    when:
    def response = mockMvc
      .perform(get("/plugins?enabled=true")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn()
      .response
      .contentAsByteArray

    and:
    Collection<Plugin> result = objectMapper.readValue(
      response, new TypeReference<Collection<Plugin>>() {})

    then:
    assert result.first().name == plugin.name
    assert result.first().namespace == plugin.namespace
  }
}
