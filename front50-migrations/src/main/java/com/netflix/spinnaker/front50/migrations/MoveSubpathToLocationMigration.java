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

package com.netflix.spinnaker.front50.migrations;

import com.google.common.base.Strings;
import com.netflix.spinnaker.front50.model.ItemDAO;
import com.netflix.spinnaker.front50.model.pipeline.Pipeline;
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO;
import java.time.Clock;
import java.util.*;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MoveSubpathToLocationMigration implements Migration {

  private static final Logger log = LoggerFactory.getLogger(CloudProvidersStringMigration.class);

  // Only valid until October 1st, 2020
  private static final Date VALID_UNTIL = new GregorianCalendar(2020, 10, 1).getTime();

  private Clock clock = Clock.systemDefaultZone();

  private final PipelineDAO pipelineDAO;

  @Autowired
  public MoveSubpathToLocationMigration(PipelineDAO pipelineDAO) {
    this.pipelineDAO = pipelineDAO;
  }

  @Override
  public boolean isValid() {
    return clock.instant().toEpochMilli() < VALID_UNTIL.getTime();
  }

  @Override
  public void run() {
    log.info("Starting git/repo artifact migration");

    Predicate<Pipeline> hasExpectedGitRepoArtifact =
        p -> {
          List<Map> expectedArtifacts = (List<Map>) p.get("expectedArtifacts");
          if (expectedArtifacts == null) {
            return false;
          }

          return expectedArtifacts.stream()
              .anyMatch(
                  e -> {
                    Map matchArtifact = (Map<String, Object>) e.get("matchArtifact");
                    Map defaultArtifact = (Map<String, Object>) e.get("defaultArtifact");

                    return matchArtifact != null
                            && isGitRepoArtifact(matchArtifact)
                            && hasSubpathInMetadata(matchArtifact)
                        || defaultArtifact != null
                            && isGitRepoArtifact(defaultArtifact)
                            && hasSubpathInMetadata(defaultArtifact);
                  });
        };

    pipelineDAO.all().stream()
        .filter(hasExpectedGitRepoArtifact)
        .forEach(pipeline -> migrate(pipelineDAO, pipeline));
  }

  private boolean isGitRepoArtifact(Map artifact) {
    return "git/repo".equalsIgnoreCase((String) artifact.get("type"));
  }

  private boolean hasSubpathInMetadata(Map artifact) {
    Map<String, Object> metadata = (Map<String, Object>) artifact.get("metadata");
    if (metadata != null) {
      String location = Strings.nullToEmpty((String) artifact.get("location"));
      String subpath = Strings.nullToEmpty((String) metadata.get("subPath"));
      return !subpath.isEmpty() && location.isEmpty();
    } else {
      return false;
    }
  }

  private void migrate(ItemDAO<Pipeline> dao, Pipeline pipeline) {

    log.info(
        "Move git/repo subpath to location field (application: {}, pipelineId: {}, expectedArtifacts: {})",
        pipeline.getApplication(),
        pipeline.getId(),
        pipeline.get("expectedArtifacts"));

    List<Map<String, Object>> expectedArtifacts =
        (List<Map<String, Object>>) pipeline.get("expectedArtifacts");
    for (Map<String, Object> expectedArtifact : expectedArtifacts) {
      Map<String, Object> matchArtifact =
          (Map<String, Object>) expectedArtifact.get("matchArtifact");
      if (matchArtifact != null
          && isGitRepoArtifact(matchArtifact)
          && hasSubpathInMetadata(matchArtifact)) {
        Map<String, Object> matchArtifactMetadata =
            (Map<String, Object>) matchArtifact.get("metadata");
        matchArtifact.put("location", matchArtifactMetadata.get("subPath"));
      }

      Map<String, Object> defaultArtifact =
          (Map<String, Object>) expectedArtifact.get("defaultArtifact");
      if (defaultArtifact != null
          && isGitRepoArtifact(defaultArtifact)
          && hasSubpathInMetadata(defaultArtifact)) {
        Map<String, Object> defaultArtifactMetadata =
            (Map<String, Object>) defaultArtifact.get("metadata");
        defaultArtifact.put("location", defaultArtifactMetadata.get("subPath"));
      }
    }
    dao.update(pipeline.getId(), pipeline);
  }
}
