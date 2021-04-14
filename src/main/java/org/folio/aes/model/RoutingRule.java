package org.folio.aes.model;

import java.util.Objects;

/**
 * A routing rule is used to determine which messages will be routed via AES.
 */
public class RoutingRule {

  private final String criteria;
  private final String target;

  public RoutingRule(String criteria, String target) {
    this.criteria = criteria;
    this.target = target;
  }

  @Override
  public String toString() {
    return "RoutingRule [criteria=" + criteria + ", target=" + target + "]";
  }

  public String getCriteria() {
    return criteria;
  }

  public String getTarget() {
    return target;
  }

  @Override
  public int hashCode() {
    return Objects.hash(criteria, target);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RoutingRule)) {
      return false;
    }
    RoutingRule other = (RoutingRule) obj;
    return Objects.equals(criteria, other.criteria) && Objects.equals(target, other.target);
  }
}
