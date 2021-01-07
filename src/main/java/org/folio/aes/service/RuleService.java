package org.folio.aes.service;

import io.vertx.core.Future;
import java.util.Collection;
import org.folio.aes.model.RoutingRule;

public interface RuleService {
  Future<Collection<RoutingRule>> getRules(String okapiUrl, String tenant, String token);

  void stop();
}
