/*
 * Copyright 2019 Netflix, Inc.
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

package com.netflix.spinnaker.front50.model

import com.netflix.spinnaker.front50.model.tag.EntityTags
import com.netflix.spinnaker.kork.dynamicconfig.DynamicConfigService
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.*
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal object CompositeStorageServiceTests : JUnit5Minutests {
  val dynamicConfigService: DynamicConfigService = mockk(relaxUnitFun = true)
  val primary: StorageService = mockk(relaxUnitFun = true)
  val previous: StorageService = mockk(relaxUnitFun = true)

  val subject = CompositeStorageService(dynamicConfigService, primary, previous)

  fun tests() = rootContext {
    after {
      clearMocks(dynamicConfigService, primary, previous)
    }

    context("Entity Tags") {
      test("should always load from 'previous'") {
        every {
          previous.loadObject<EntityTags>(ObjectType.ENTITY_TAGS, "id-entitytags001")
        } returns EntityTags().apply { id = "id-entitytags001" }

        expectThat(
          subject.loadObject<EntityTags>(ObjectType.ENTITY_TAGS, "id-entitytags001").id
        ).isEqualTo("id-entitytags001")

        verifyAll {
          primary wasNot Called
          previous.loadObject<Timestamped>(ObjectType.ENTITY_TAGS, "id-entitytags001")
        }
      }
    }
  }
}
