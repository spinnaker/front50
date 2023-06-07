/*
 * Copyright 2023 Salesforce, Inc.
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
 *
 */

package com.netflix.spinnaker.front50.model.pipeline

import com.netflix.spinnaker.front50.api.model.pipeline.Pipeline
import com.netflix.spinnaker.front50.pipeline.PipelineDAOSpec
import com.netflix.spinnaker.front50.pipeline.SqlPipelineDAOTestConfiguration
import com.netflix.spinnaker.kork.sql.test.SqlTestUtil
import spock.lang.AutoCleanup

abstract class DefaultPipelineDAOSpec extends PipelineDAOSpec<DefaultPipelineDAO> {
  DefaultPipelineDAO pipelineDAO

  @Override
  DefaultPipelineDAO getInstance() {
    return getDefaultPipelineDAO()
  }

  abstract DefaultPipelineDAO getDefaultPipelineDAO()

  void setup() {
    this.pipelineDAO = Spy(getDefaultPipelineDAO())
  }

  void "should return correct pipelines when requesting pipelines by application with name filter"() {
    given:
    def pipeline = new Pipeline(id: "0", application: applicationName, name: pipelineName)
    // it's hard to use the pipelineDAO to actually insert null pipeline names, so use a spy to mock the values in cache
    1 * pipelineDAO.all(true) >> [pipeline]

    when:
    def pipelines = pipelineDAO.getPipelinesByApplication("app", pipelineNameFilter, true)

    then:
    pipelines[0].getName() == expectedPipelineName
    pipelines[0].getApplication() == "app"

    where:
    applicationName | pipelineName    | pipelineNameFilter || expectedPipelineName
    "app"           | "pipelineNameA" | "NameA"            || "pipelineNameA"
    "app"           | "pipelineNameA" | null               || "pipelineNameA"
    "app"           | null            | null               || null
  }

  void "should return no pipelines when requesting pipelines by application with name filter"() {
    given:
    def pipeline = new Pipeline(id: "0", application: applicationName, name: pipelineName)
    // it's hard to use the pipelineDAO to actually insert null pipeline names, so use a spy to mock the values in cache
    1 * pipelineDAO.all(true) >> [pipeline]

    when:
    def pipelines = pipelineDAO.getPipelinesByApplication("app", pipelineNameFilter, true)

    then:
    pipelines.size() == 0

    where:
    applicationName | pipelineName    | pipelineNameFilter
    "app"           | null            | "NameA"
    "bad"           | "pipelineNameA" | "NameA"
    "bad"           | null            | "NameA"
    "bad"           | "pipelineNameA" | null
    "bad"           | null            | null
  }
}

class SqlDefaultPipelineDAOSpec extends DefaultPipelineDAOSpec {
  @AutoCleanup("close")
  SqlTestUtil.TestDatabase database = SqlTestUtil.initTcMysqlDatabase()

  def cleanup() {
    if (database != null) {
      SqlTestUtil.cleanupDb(database.context)
    }
  }

  DefaultPipelineDAO defaultPipelineDAO = SqlPipelineDAOTestConfiguration.createPipelineDAO(database)
}
