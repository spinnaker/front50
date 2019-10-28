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

import com.netflix.spinnaker.front50.model.flushAll
import com.netflix.spinnaker.front50.model.initDatabase
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.GetCriteria
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.ListCriteria
import com.netflix.spinnaker.front50.model.plugin.PluginRepository.UpsertData
import com.netflix.spinnaker.front50.proto.Plugin
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import org.jooq.SQLDialect
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.assertDoesNotThrow
import strikt.api.expect
import strikt.api.expectThrows

internal object SqlPluginRepositoryTests : JUnit5Minutests {
  private val jooq = initDatabase(
    "jdbc:tc:mysql:5.7.22://somehostname:someport/databasename",
    SQLDialect.MYSQL
  )

  private val sqlPluginRepository = SqlPluginRepository(jooq)
  private val pluginBuilder = Plugin.newBuilder()
    .setName("name")
    .setNamespace("namespace")
    .setVersion("1.0.0")
    .setAuthor("author")
    .addAllCapabilities(mutableListOf("a-capability", "another-capability"))
    .addAllRequiredVersions(mutableListOf("clouddriver>=1", "orca>=2"))
    .addAllDependencies(mutableListOf("a-necessary-thing"))
    .setEnabled(true)
    .setInProcessUnsafe(false)

  fun tests() = rootContext {

    after {
      jooq.flushAll()
    }

    context("Upsert, get, list plugins") {

      test("Able to get a plugin") {
        val plugin = pluginBuilder.build()
        sqlPluginRepository.upsert(UpsertData(plugin, "someone"))

        val result = sqlPluginRepository.get(GetCriteria(plugin.namespace, plugin.name))

        expect {
          assert(result == plugin)
        }
      }

      test("throws NotFoundException when plugin does not exist") {
        expectThrows<NotFoundException> {
          val criteria = GetCriteria("foo", "bar")
          sqlPluginRepository.get(criteria)
        }
      }

      test("Able to upsert a new plugin") {
        val plugin = pluginBuilder.build()

        expect {
          assertDoesNotThrow {
            sqlPluginRepository.upsert(UpsertData(plugin, "someone"))
          }
        }
      }

      test("Able to upsert an existing plugin and change a field") {
        val plugin = pluginBuilder.build()
        val updatedPlugin = pluginBuilder.setAuthor("new-author").build()

        sqlPluginRepository.upsert(UpsertData(plugin, "someone"))
        sqlPluginRepository.upsert(UpsertData(updatedPlugin, "someone"))

        expect {
          val pluginsResult = sqlPluginRepository.list(ListCriteria(true))
          assert(pluginsResult.size == 1)

          val pluginResult = sqlPluginRepository.get(GetCriteria(updatedPlugin.namespace, updatedPlugin.name))
          assert(pluginResult == updatedPlugin)
        }
      }

      test("Able to list enabled and disabled plugins") {
        val enabledPlugin = pluginBuilder.build()
        val disabledPlugin = pluginBuilder
          .setName("other-name")
          .setNamespace("other-namespace")
          .setEnabled(false)
          .build()

        sqlPluginRepository.upsert(UpsertData(enabledPlugin, "someone"))
        sqlPluginRepository.upsert(UpsertData(disabledPlugin, "someone"))

        mutableListOf(true, false).forEach { enabled ->
          val pluginsResult = sqlPluginRepository.list(ListCriteria(enabled))
          expect {
            assert(pluginsResult.size == 1)
            if (enabled) assert(pluginsResult.first().enabled)
            else assert(!pluginsResult.first().enabled)
          }
        }
      }
    }
  }

  @JvmStatic
  @AfterAll
  fun shutdown() {
    jooq.close()
  }
}
