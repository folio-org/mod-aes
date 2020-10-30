package org.folio.aes.service;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.folio.aes.model.RoutingRule;

public interface RuleService {

  /**
   * Retrieve {@link RoutingRule}s.
   *
   * @param okapiUrl
   * @param tenant
   * @param token
   * @return
   */
  CompletableFuture<Collection<RoutingRule>> getRules(String okapiUrl, String tenant, String token);

  void stop();
}
