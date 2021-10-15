package com.netflix.spinnaker.front50.migrations;

import com.netflix.spinnaker.front50.api.model.pipeline.Pipeline;
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SpelLoadBalancersMigration implements Migration {
  // Only valid until October 13th, 2021
  private static final Date VALID_UNTIL = new GregorianCalendar(2021, 10, 13).getTime();
  private Clock clock = Clock.systemDefaultZone();

  private final PipelineDAO pipelineDAO;

  public SpelLoadBalancersMigration(PipelineDAO pipelineDAO) {
    this.pipelineDAO = pipelineDAO;
  }

  @Override
  public boolean isValid() {
    return clock.instant().toEpochMilli() < VALID_UNTIL.getTime();
  }

  public void run() {
    log.info("Starting spelLoadBalancers migration");
    Collection<Pipeline> pipelines = pipelineDAO.all();
    int migratedCount = 0;
    int failureCount = 0;
    for (Pipeline pipeline : pipelines) {
      try {
        if (migrate(pipeline)) {
          migratedCount++;
        }
      } catch (Exception e) {
        log.error(
            "Failed to migrate pipeline {} ({}) for {} spelLoadBalancersMigration",
            pipeline.getName(),
            pipeline.getId(),
            pipeline.getApplication(),
            e);
        failureCount++;
      }
    }
    log.info(
        "Done with spelLoadBalancers migration (migrated {} pipelines; {} failed to migrate)",
        migratedCount,
        failureCount);
  }

  /** Removes spelLoadBalancers and spelTargetGroups from all deploy stage[].clusters[] */
  private boolean migrate(Pipeline pipeline) {
    List<Map<String, Object>> stages = pipeline.getStages();
    if (stages == null) {
      stages = Collections.emptyList();
    }
    List<Map<String, Object>> clusters =
        stages.stream()
            .filter(stage -> "deploy".equals(stage.get("type")))
            .flatMap(
                stage ->
                    ((List<Map<String, Object>>)
                            stage.getOrDefault("clusters", Collections.emptyList()))
                        .stream())
            .filter(
                cluster ->
                    cluster.get("spelLoadBalancers") != null
                        || cluster.get("spelTargetGroups") != null)
            .collect(Collectors.toList());

    if (clusters.isEmpty()) {
      return false;
    }

    for (Map<String, Object> cluster : clusters) {
      cluster.remove("spelLoadBalancers");
      cluster.remove("spelTargetGroups");
    }
    pipelineDAO.update(pipeline.getId(), pipeline);
    log.info(
        "Migrated pipeline {} ({}) for {} spelLoadBalancersMigration",
        pipeline.getName(),
        pipeline.getId(),
        pipeline.getApplication());

    return true;
  }
}
