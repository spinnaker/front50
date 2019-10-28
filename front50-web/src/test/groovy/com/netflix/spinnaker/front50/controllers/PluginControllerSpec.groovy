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

import com.netflix.spinnaker.front50.model.plugin.PluginRepository
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.GetCriteria
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.ListCriteria
import com.netflix.spinnaker.front50.proto.Plugin
import com.netflix.spinnaker.front50.proto.Plugins
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver
import spock.lang.Specification
import spock.lang.Subject

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class PluginControllerSpec extends Specification {

  Plugin.Builder pluginBuilder

  Plugin plugin

  PluginRepository<Plugin> pluginRepository

  MockMvc mockMvc

  @Subject
  PluginController controller

  void setup() {
    pluginBuilder = Plugin.newBuilder()
      .setName("name")
      .setNamespace("namespace")
      .setVersion("1.0.0")
      .setAuthor("author")
      .addAllCapabilities(["a-capability", "another-capability"])
      .addAllRequiredVersions(["clouddriver>=1", "orca>=2"])
      .addAllDependencies(["a-necessary-thing"])
      .setEnabled(true)
      .setInProcessUnsafe(false)

    plugin = pluginBuilder.build()

    pluginRepository = Stub(PluginRepository)

    controller = new PluginController(pluginRepository)

    mockMvc = MockMvcBuilders
      .standaloneSetup(controller)
      .setHandlerExceptionResolvers(createExceptionResolver())
      .setMessageConverters([new ProtobufHttpMessageConverter()] as HttpMessageConverter<?>[])
      .build()
  }

  private static ExceptionHandlerExceptionResolver createExceptionResolver() {
    def resolver = new SimpleExceptionHandlerExceptionResolver()
    resolver.afterPropertiesSet()
    return resolver
  }

  def "mvcmock - post plugin"() {
    when:
    def response = mockMvc
      .perform(post("/plugins")
        .contentType("application/x-protobuf")
        .content(plugin.toByteArray()))
      .andExpect(status().isOk())
      .andReturn()
      .response

    then:
    assert response.status == 200
  }

  def "mvcmock - get plugin"() {
    given:
    pluginRepository.get(_ as GetCriteria) >> plugin

    when:
    def response = mockMvc
      .perform(get("/plugins/namespace/name")
        .contentType("application/x-protobuf"))
      .andExpect(status().isOk())
      .andReturn()
      .response
      .contentAsByteArray

    Plugin result = Plugin.parseFrom(response)

    then:
    assert plugin == result
  }

  def "mvcmock - get a plugin that doesn't exist"() {
    given:
    pluginRepository.get(_ as GetCriteria) >> { throw new NotFoundException() }

    when:
    def response = mockMvc
      .perform(get("/plugins/namespace/name")
        .contentType("application/x-protobuf"))
      .andExpect(status().is4xxClientError())
      .andReturn()
      .response

    then:
    assert response.status == 404
  }

  def "mvcmock - list plugins"() {
    given:
    pluginRepository.list(_ as ListCriteria) >> [plugin]

    when:
    def response = mockMvc
      .perform(get("/plugins?enabled=true")
        .contentType("application/x-protobuf"))
      .andExpect(status().isOk())
      .andReturn()
      .response
      .contentAsByteArray

    and:
    Plugins result = Plugins.parseFrom(response)
    List<Plugin> responseList = result.pluginList

    then:
    assert responseList == [plugin]
  }

  def "gets a plugin"() {
    when:
    pluginRepository.get(_ as GetCriteria) >> plugin

    and:
    def result = controller.get(plugin.namespace, plugin.name)

    then:
    assert result == plugin
  }

  def "lists plugins"() {
    when:
    pluginRepository.list(_ as ListCriteria) >> [plugin]

    and:
    Plugins result = controller.list(true)
    List<Plugin> pluginList = result.getPluginList()

    then:
    assert pluginList == [plugin]
  }

  def "upserts a plugin"() {
    when:
    controller.upsert(plugin)

    then:
    noExceptionThrown()
  }
}
