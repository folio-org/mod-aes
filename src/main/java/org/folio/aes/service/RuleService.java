package org.folio.aes.service;

import java.util.Collection;

import org.folio.aes.model.RoutingRule;

import io.vertx.core.Future;

public interface RuleService {

  /**
   * Retrieve {@link RoutingRule}s.
   *
   * @param okapiUrl
   * @param tenant
   * @param token
   * @return
   */
  Future<Collection<RoutingRule>> getRules(String okapiUrl, String tenant, String token);

  void stop();
}
