package com.netflix.spinnaker.front50.model.pipeline;

import com.netflix.spinnaker.front50.api.model.pipeline.Pipeline;
import com.netflix.spinnaker.front50.model.ItemDAO;
import java.util.Collection;

public interface PipelineStrategyDAO extends ItemDAO<Pipeline> {
  String getPipelineId(String application, String pipelineName);

  Collection<Pipeline> getPipelinesByApplication(String application);
}
