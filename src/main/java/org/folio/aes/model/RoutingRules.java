package org.folio.aes.model;

import java.util.ArrayList;
import java.util.List;

public class RoutingRules {

  private List<RoutingRule> rules = new ArrayList<>();
  private long timestamp = System.currentTimeMillis();

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public List<RoutingRule> getRules() {
    return rules;
  }

  public void setRules(List<RoutingRule> rules) {
    this.rules = rules;
  }

}
