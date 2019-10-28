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

import io.github.resilience4j.retry.annotation.Retry
import org.jooq.DSLContext
import org.jooq.impl.DSL

// TODO(csmalley): Unify front50 around this usage of resilience4j

@Retry(name = "sqlTransaction")
internal fun DSLContext.transactional(fn: (DSLContext) -> Unit) {
  transaction { ctx ->
    fn(DSL.using(ctx))
  }
}

@Retry(name = "sqlRead")
internal fun <T> DSLContext.read(fn: (DSLContext) -> T): T {
  return fn(this)
}
