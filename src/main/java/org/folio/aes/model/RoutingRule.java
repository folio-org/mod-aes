package org.folio.aes.model;

public class RoutingRule {

  private String criteria;
  private String target;

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

  public void setCriteria(String criteria) {
    this.criteria = criteria;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

}
