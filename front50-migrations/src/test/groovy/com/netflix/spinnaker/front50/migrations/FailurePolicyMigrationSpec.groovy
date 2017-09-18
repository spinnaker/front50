/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.netflix.spinnaker.front50.migrations

import java.util.concurrent.atomic.AtomicReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.front50.model.pipeline.Pipeline
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO
import com.netflix.spinnaker.front50.model.pipeline.PipelineStrategyDAO
import spock.lang.Specification
import spock.lang.Subject

class FailurePolicyMigrationSpec extends Specification {

  def pipelineDAO = Mock(PipelineDAO)
  def strategyDAO = Stub(PipelineStrategyDAO) {
    all() >> []
  }

  @Subject migration = new FailurePolicyMigration()

  def setup() {
    migration.pipelineDAO = pipelineDAO
    migration.pipelineStrategyDAO = strategyDAO
  }

  def "migrates a FAILED_CONTINUE stage"() {
    given:
    pipelineDAO.all() >> [pipeline]

    and:
    def updated = new AtomicReference<Map<String, ?>>()
    pipelineDAO.update(pipeline.id, _) >> { id, p -> updated.set(p) }

    when:
    migration.run()

    then:
    with(updated.get()) {
      stages[0].onFailure == "ignore"
      stages[1].onFailure == "fail"
      stages.every {
        !it.containsKey("failPipeline")
      }
    }

    where:
    json = [
      id                    : UUID.randomUUID().toString(),
      "appConfig"           : [:],
      "keepWaitingPipelines": false,
      "lastModifiedBy"      : "rfletcher@netflix.com",
      "limitConcurrent"     : true,
      "parallel"            : true,
      "stages"              : [
        [
          "continuePipeline"        : true,
          "failPipeline"            : false,
          "job"                     : "faily-godmother",
          "markUnstableAsSuccessful": true,
          "master"                  : "spinnaker",
          "name"                    : "Jenkins",
          "notifications"           : [
            [
              "address": "echotest",
              "level"  : "stage",
              "type"   : "slack",
              "when"   : [
                "stage.starting",
                "stage.complete",
                "stage.failed"
              ]
            ]
          ],
          "parameters"              : [:],
          "refId"                   : "1",
          "requisiteStageRefIds"    : [],
          "sendNotifications"       : true,
          "type"                    : "jenkins",
          "waitForCompletion"       : true
        ],
        [
          "name"                : "Wait",
          "notifications"       : [
            [
              "address": "echotest",
              "level"  : "stage",
              "type"   : "slack",
              "when"   : [
                "stage.starting",
                "stage.complete",
                "stage.failed"
              ]
            ]
          ],
          "refId"               : "2",
          "requisiteStageRefIds": ["1"],
          "sendNotifications"   : true,
          "type"                : "wait",
          "waitTime"            : 2
        ]
      ],
      "triggers"            : [],
      "updateTs"            : "1493939777000"
    ]
    pipeline = new ObjectMapper().convertValue(json, Pipeline)
  }
}
