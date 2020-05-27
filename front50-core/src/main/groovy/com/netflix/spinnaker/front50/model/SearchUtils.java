package com.netflix.spinnaker.front50.model;

import com.netflix.spinnaker.front50.UntypedUtils;
import java.util.Map;
import java.util.TreeMap;

public class SearchUtils {

  public static boolean matchesIgnoreCase(Map<String, String> source, String key, String value) {
    Map<String, String> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    treeMap.putAll(source);
    return treeMap.containsKey(key) && treeMap.get(key).toLowerCase().contains(value.toLowerCase());
  }

  public static int score(final Timestamped timestamped, Map<String, String> attributes) {
    return attributes.entrySet().stream()
        .map(e -> score(timestamped, e.getKey(), e.getValue()))
        .reduce(Integer::sum)
        .orElse(0);
  }

  public static int score(Timestamped timestamped, String attributeName, String attributeValue) {
    Map<String, String> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    treeMap.putAll(UntypedUtils.getProperties(timestamped));

    if (!treeMap.containsKey(attributeName)) {
      return 0;
    }

    String attribute = treeMap.get(attributeName).toLowerCase();
    int indexOf = attribute.indexOf(attributeValue.toLowerCase());

    // what percentage of the value matched
    double coverage = ((double) attributeValue.length() / attribute.length()) * 100;

    // where did the match occur, bonus points for it occurring close to the start
    int boost = attribute.length() - indexOf;

    // scale boost based on coverage percentage
    double scaledBoost = (coverage / 100) * boost;

    return (int) (indexOf < 0 ? 0 : coverage + scaledBoost);
  }
}
