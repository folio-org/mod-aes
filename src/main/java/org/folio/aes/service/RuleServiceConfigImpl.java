package org.folio.aes.service;

import static org.folio.aes.util.AesConstants.CONFIG_CONFIGS;
import static org.folio.aes.util.AesConstants.CONFIG_ROUTING_CRITERIA;
import static org.folio.aes.util.AesConstants.CONFIG_ROUTING_TARGET;
import static org.folio.aes.util.AesConstants.CONFIG_VALUE;
import static org.folio.aes.util.AesConstants.OKAPI_AES_FILTER_ID;
import static org.folio.aes.util.AesConstants.OKAPI_TENANT;
import static org.folio.aes.util.AesConstants.OKAPI_TOKEN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.aes.model.RoutingRule;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Retrieve and cache {@link RoutingRule}s from mod-config.
 */
public class RuleServiceConfigImpl implements RuleService {

  private static Logger logger = LogManager.getLogger();

  private static final long CACHE_PERIOD = 1 * 60 * 1000L; // one minute

  private final WebClient client;
  private final ConcurrentMap<String, Future<Collection<RoutingRule>>> cachedRules;
  private final Vertx vertx;

  public RuleServiceConfigImpl(Vertx vertx) {
    this.vertx = vertx;
    this.client = WebClient.create(vertx);
    this.cachedRules = new ConcurrentHashMap<>();
  }

  @Override
  public Future<Collection<RoutingRule>> getRules(String okapiUrl, String tenant, String token) {
    return cachedRules.computeIfAbsent(tenant, key -> {
      logger.info("Refresh routing rules for tenant {}", key);
      return getFreshRules(okapiUrl, key, token)
          .onSuccess(rules -> vertx.setTimer(CACHE_PERIOD, id -> cachedRules.remove(key)))
          .onFailure(e -> cachedRules.remove(key));
    });
  }

  @Override
  public void stop() {
    client.close();
  }

  private Future<Collection<RoutingRule>> getFreshRules(String okapiUrl, String tenant, String token) {
    final Promise<Collection<RoutingRule>> promise = Promise.promise();
    HttpRequest<Buffer> request = client.getAbs(okapiUrl);
    request.putHeader("Accept", "application/json").putHeader(OKAPI_AES_FILTER_ID, "true");
    if (tenant != null) {
      request.putHeader(OKAPI_TENANT, tenant);
    }
    if (token != null) {
      request.putHeader(OKAPI_TOKEN, token);
    }
    request.send(ar -> {
      if (ar.succeeded()) {
        HttpResponse<Buffer> response = ar.result();
        if (response.statusCode() == 200) {
          Collection<RoutingRule> rules = new ArrayList<>();
          response.bodyAsJsonObject().getJsonArray(CONFIG_CONFIGS)
            .forEach(item -> {
              JsonObject jo = new JsonObject(((JsonObject) item).getString(CONFIG_VALUE));
              String topic = String.format("%s_%s", tenant, jo.getString(CONFIG_ROUTING_TARGET));
              rules.add(new RoutingRule(jo.getString(CONFIG_ROUTING_CRITERIA), topic));
            });
          promise.complete(rules);
        } else {
          String errorMsg = response.statusCode() + " " + response.bodyAsString();
          logger.warn(errorMsg);
          promise.fail(errorMsg);
        }
      } else {
        logger.warn(ar.cause());
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }
}
