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

package com.netflix.spinnaker.front50.migrations;

import java.util.List;
import java.util.Map;
import com.netflix.spinnaker.front50.model.ItemDAO;
import com.netflix.spinnaker.front50.model.pipeline.Pipeline;
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO;
import com.netflix.spinnaker.front50.model.pipeline.PipelineStrategyDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StageTimeoutMigration implements Migration {

  private final Logger log = LoggerFactory.getLogger(getClass());
  @Autowired PipelineDAO pipelineDAO;
  @Autowired PipelineStrategyDAO pipelineStrategyDAO;

  @Override public boolean isValid() {
    return true;
  }

  @Override public void run() {
    pipelineDAO.all().stream()
      .filter(this::anyStageSpecifiesTimeout)
      .forEach(pipeline -> migrate(pipelineDAO, pipeline, "pipeline"));
    pipelineStrategyDAO.all().stream()
      .filter(this::anyStageSpecifiesTimeout)
      .forEach(pipeline -> migrate(pipelineStrategyDAO, pipeline, "strategy"));
  }

  private boolean anyStageSpecifiesTimeout(Pipeline pipeline) {
    return getStages(pipeline)
      .stream()
      .anyMatch(stage -> stage.containsKey("stageTimeoutMs"));
  }

  private void migrate(ItemDAO<Pipeline> dao, Pipeline pipeline, String type) {
    getStages(pipeline)
      .stream()
      .filter(stage -> stage.containsKey("stageTimeoutMs"))
      .forEach(stage -> stage.put("timeoutMs", stage.remove("stageTimeoutMs")));
    dao.update(pipeline.getId(), pipeline);
    log.info("Migrated {} {} stage timeouts", type, pipeline.getId());
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getStages(Pipeline pipeline) {
    return (List<Map<String, Object>>) pipeline.get("stages");
  }
}
