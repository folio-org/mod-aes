package org.folio.aes.model;

public class RoutingRule {

  public RoutingRule(String criteria, String target) {
    super();
    this.criteria = criteria;
    this.target = target;
  }

  @Override
  public String toString() {
    return "RoutingRule [criteria=" + criteria + ", target=" + target + "]";
  }

  private String criteria;
  private String target;

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
