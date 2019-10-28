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

package com.netflix.spinnaker.front50.model.plugin

import com.netflix.spinnaker.front50.model.plugin.PluginRepository.UpsertData
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.GetCriteria
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.ListCriteria
import com.netflix.spinnaker.front50.proto.Plugin
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.SelectJoinStep
import org.jooq.impl.DSL.currentTimestamp
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.table
import org.slf4j.LoggerFactory
import java.sql.Timestamp

class SqlPluginRepository(
  private val jooq: DSLContext
) : PluginRepository<Plugin> {

  override fun upsert(data: UpsertData<Plugin>) =
    jooq.transactional { ctx ->
      val plugin = data.plugin
      val modifiedBy = data.modifiedBy
      val timestamp = ctx.fetchValue(select(currentTimestamp()))

      ctx.insertInto(
        PLUGINS,
        NAME,
        NAMESPACE,
        DESCRIPTION,
        AUTHOR,
        VERSION,
        REQUIRED_VERSIONS,
        DEPENDENCIES,
        CAPABILITIES,
        ENABLED,
        IN_PROCESS_UNSAFE,
        CREATED_AT,
        LAST_MODIFIED_AT,
        LAST_MODIFIED_BY
      ).values(
        plugin.name,
        plugin.namespace,
        plugin.description,
        plugin.author,
        plugin.version,
        plugin.requiredVersionsList.commaDelimited(),
        plugin.dependenciesList.commaDelimited(),
        plugin.capabilitiesList.commaDelimited(),
        plugin.enabled,
        plugin.inProcessUnsafe,
        timestamp,
        timestamp,
        modifiedBy
      ).onDuplicateKeyUpdate()
        .set(DESCRIPTION, plugin.description)
        .set(AUTHOR, plugin.author)
        .set(VERSION, plugin.version)
        .set(REQUIRED_VERSIONS, plugin.requiredVersionsList.commaDelimited())
        .set(DEPENDENCIES, plugin.dependenciesList.commaDelimited())
        .set(CAPABILITIES, plugin.capabilitiesList.commaDelimited())
        .set(ENABLED, plugin.enabled)
        .set(IN_PROCESS_UNSAFE, plugin.inProcessUnsafe)
        .set(LAST_MODIFIED_AT, timestamp)
        .set(LAST_MODIFIED_BY, modifiedBy)
        .execute()
    }

  override fun get(criteria: GetCriteria): Plugin =
    jooq.read { ctx ->
      ctx.selectFromPlugins()
        .where(NAME.eq(criteria.name))
        .and(NAMESPACE.eq(criteria.namespace))
        .fetchOptional()
        .orElseThrow {
          NotFoundException("Plugin not found - " +
            "name: ${criteria.name}, " +
            "namespace: ${criteria.namespace}")
        }
        .toPlugin()
    }

  override fun list(criteria: ListCriteria): List<Plugin> =
    jooq.read { ctx ->
      ctx.selectFromPlugins()
        .where(ENABLED.eq(criteria.enabled))
        .fetch()
        .map { it.toPlugin() }
    }

  private fun Record.toPlugin(): Plugin =
    Plugin.newBuilder()
      .setName(this.get(NAME))
      .setNamespace(this.get(NAMESPACE))
      .setDescription(this.get(DESCRIPTION))
      .setAuthor(this.get(AUTHOR))
      .setVersion(this.get(VERSION))
      .addAllRequiredVersions(this.get(REQUIRED_VERSIONS).commaDelimitedToList())
      .addAllDependencies(this.get(DEPENDENCIES).commaDelimitedToList())
      .addAllCapabilities(this.get(CAPABILITIES).commaDelimitedToList())
      .setEnabled(this.get(ENABLED))
      .setInProcessUnsafe(this.get(IN_PROCESS_UNSAFE))
      .build()

  private fun DSLContext.selectFromPlugins(): SelectJoinStep<*> =
    this.select(
      NAME,
      NAMESPACE,
      DESCRIPTION,
      AUTHOR,
      VERSION,
      REQUIRED_VERSIONS,
      DEPENDENCIES,
      CAPABILITIES,
      ENABLED,
      IN_PROCESS_UNSAFE
    ).from(PLUGINS)

  private fun List<String>.commaDelimited() = this.joinToString(",")

  private fun String.commaDelimitedToList(): List<String> =
    this.split(",").map { it.trim() }

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  companion object {
    private val PLUGINS = table("plugins")

    private val NAME = field("name", String::class.java)
    private val NAMESPACE = field("namespace", String::class.java)
    private val DESCRIPTION = field("description", String::class.java)
    private val AUTHOR = field("author", String::class.java)
    private val VERSION = field("version", String::class.java)
    private val REQUIRED_VERSIONS = field("required_versions", String::class.java)
    private val DEPENDENCIES = field("dependencies", String::class.java)
    private val CAPABILITIES = field("capabilities", String::class.java)
    private val ENABLED = field("enabled", Boolean::class.java)
    private val IN_PROCESS_UNSAFE = field("in_process_unsafe", Boolean::class.java)
    private val CREATED_AT = field("created_at", Timestamp::class.java)
    private val LAST_MODIFIED_AT = field("last_modified_at", Timestamp::class.java)
    private val LAST_MODIFIED_BY = field("last_modified_by", String::class.java)
  }
}
