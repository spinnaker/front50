package com.netflix.spinnaker.front50.model.application;

import com.google.common.base.Strings;
import com.netflix.spinnaker.front50.UntypedUtils;
import com.netflix.spinnaker.front50.exception.NotFoundException;
import com.netflix.spinnaker.front50.model.ItemDAO;
import com.netflix.spinnaker.front50.model.SearchUtils;
import java.util.*;
import java.util.stream.Collectors;

public interface ApplicationDAO extends ItemDAO<Application> {
  Application findByName(String name) throws NotFoundException;

  Collection<Application> search(Map<String, String> attributes);

  class Searcher {
    public static Collection<Application> search(
        Collection<Application> searchableApplications, Map<String, String> attributes) {
      Map<String, String> normalizedAttributes = new HashMap<>();
      attributes.forEach((key, value) -> normalizedAttributes.put(key.toLowerCase(), value));

      // filtering vs. querying to achieve case-insensitivity without using an additional column
      // (small data set)
      return searchableApplications.stream()
          .filter(
              it -> {
                for (Map.Entry<String, String> e : normalizedAttributes.entrySet()) {
                  if (Strings.isNullOrEmpty(e.getValue())) {
                    continue;
                  }
                  if (!UntypedUtils.hasProperty(it, e.getKey())
                      && !it.details().containsKey(e.getKey())) {
                    return false;
                  }

                  Object appVal =
                      UntypedUtils.hasProperty(it, e.getKey())
                          ? UntypedUtils.getProperty(it, e.getKey())
                          : it.details().get(e.getKey());
                  if (appVal == null) {
                    appVal = "";
                  }
                  if (!appVal.toString().toLowerCase().contains(e.getValue().toLowerCase())) {
                    return false;
                  }
                }
                return true;
              })
          .distinct()
          .sorted(
              (a, b) ->
                  SearchUtils.score(b, normalizedAttributes)
                      - SearchUtils.score(a, normalizedAttributes))
          .collect(Collectors.toList());
    }
  }
}
