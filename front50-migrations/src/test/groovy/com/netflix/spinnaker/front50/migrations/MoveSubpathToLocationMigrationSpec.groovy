/*
 * Copyright 2020 Netflix, Inc.
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

package com.netflix.spinnaker.front50.migrations

import com.netflix.spinnaker.front50.model.pipeline.Pipeline
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class MoveSubpathToLocationMigrationSpec extends Specification {
  def pipelineDAO = Mock(PipelineDAO)

  @Subject
  def migration = new MoveSubpathToLocationMigration(pipelineDAO)

  @Unroll
  def "should not set subpath in location"() {
    given:
    def pipeline = new Pipeline([application: "test", expectedArtifacts: [[matchArtifact: original]]])
    pipeline.id = "pipeline-1"

    when:
    migration.run()

    then:
    _ * pipelineDAO.all() >> { return [pipeline] }
    0 * pipelineDAO.update("pipeline-1", _)

    where:
    original  || _
    [:] || _
    [type: "bitbucket/file", reference: "test-ref"] || _
    [type: "github/file", reference: "test-ref", version: "test-ver"] || _
    [type: "git/repo", reference: "test-ref", version: "test-ver"] || _
    [type: "git/repo", reference: "test-ref", version: "test-ver", location: "test-subpath"] || _
    [type: "git/repo", reference: "test-ref", version: "test-ver", metadata: [foo: "test-foo"]] || _
    [type: "git/repo", reference: "test-ref", version: "test-ver", location: "test-subpath", metadata: [foo: "test-foo"]] || _
    [type: "git/repo", reference: "test-ref", version: "test-ver", location: "test-subpath", metadata: [subPath: "test-subpath"]] || _
  }

  @Unroll
  def "should set subpath in location"() {
    given:
    def pipeline = new Pipeline([application: "test", expectedArtifacts: [[matchArtifact: original]]])
    pipeline.id = "pipeline-1"

    when:
    migration.run()

    then:
    pipeline.get("expectedArtifacts").find {true}.matchArtifact.location == "test-subpath"
    _ * pipelineDAO.all() >> { return [pipeline] }
    1 * pipelineDAO.update("pipeline-1", _)

    where:
    original  || _
    [type: "git/repo", reference: "test-ref", version: "test-ver", metadata: [subPath: "test-subpath"]] || _
    [type: "git/repo", reference: "test-ref", version: "test-ver", location: "", metadata: [subPath: "test-subpath"]] || _
  }

  @Unroll
  def "should set subpath to location with matchArtifact and defaultArtifact"() {
    given:
    def pipeline = new Pipeline([application: "test", expectedArtifacts: [[matchArtifact: match, defaultArtifact: defaul]]])
    pipeline.id = "pipeline-1"

    when:
    migration.run()
    migration.run() // subsequent migration run should be a no-op

    then:
    2 * pipelineDAO.all() >> { return [pipeline] }
    1 * pipelineDAO.update("pipeline-1", _)
    0 * _

    where:
    match  || defaul
    [type: "git/repo", metadata: [subPath: "test-subpath"]] || [type: "bitbucket/file", reference: "test-ref"]
    [type: "bitbucket/file", reference: "test-ref"] || [type: "git/repo", metadata: [subPath: "test-subpath"]]
    [type: "git/repo", metadata: [subPath: "test-subpath"]] || [type: "git/repo", metadata: [subPath: "test-subpath"]]
    [type: "git/repo", metadata: [subPath: "test-subpath"]] || [type: "git/repo"]
  }
}
