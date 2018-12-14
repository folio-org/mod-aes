package org.folio.aes.service;

import static org.folio.aes.util.AesConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.folio.aes.model.RoutingRule;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Retrieve and cache {@link RoutingRule}s from mod-config.
 */
public class RuleServiceConfigImpl implements RuleService {

  private static Logger logger = LoggerFactory.getLogger(RuleServiceConfigImpl.class);

  private static final long cachePeriod = 1 * 60 * 1000; // one minute

  private Vertx vertx;
  private ConcurrentMap<String, CachedRoutingRules> cachedRules = new ConcurrentHashMap<>();

  public RuleServiceConfigImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  private static class CachedRoutingRules {
    public long timestamp = 0L;
    public Collection<RoutingRule> rules = new ArrayList<>();

    public CachedRoutingRules(long timestamp, Collection<RoutingRule> rules) {
      this.timestamp = timestamp;
      this.rules = rules;
    }
  }

  @Override
  public CompletableFuture<Collection<RoutingRule>> getRules(String okapiUrl, String tenant, String token) {
    CompletableFuture<Collection<RoutingRule>> cf = new CompletableFuture<>();
    CachedRoutingRules cache = cachedRules.get(tenant);
    if (cache != null && (System.currentTimeMillis() - cache.timestamp < cachePeriod)) {
      cf.complete(cache.rules);
    } else {
      logger.info("Refresh routing rules for tenant " + tenant);
      getFreshRules(okapiUrl, tenant, token)
        .thenAccept(rules -> {
          cachedRules.put(tenant, new CachedRoutingRules(System.currentTimeMillis(), rules));
          cf.complete(rules);
          logger.info("Refreshed rules: " + rules);
        }).handle((res, ex) -> {
          if (ex != null) {
            logger.warn(ex);
            cf.completeExceptionally(ex);
            return ex;
          }
          return res;
        });
    }
    return cf;
  }

  private CompletableFuture<Collection<RoutingRule>> getFreshRules(String okapiUrl, String tenant, String token) {
    CompletableFuture<Collection<RoutingRule>> cf = new CompletableFuture<>();
    HttpRequest<Buffer> request = WebClient.create(vertx).getAbs(okapiUrl);
    MultiMap headers = request.headers().set("Accept", "application/json").set(OKAPI_AES_FILTER_ID, "true");
    if (tenant != null) {
      headers.set(OKAPI_TENANT, tenant);
    }
    if (token != null) {
      headers.set(OKAPI_TOKEN, token);
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
          cf.complete(rules);
        } else {
          String errorMsg = response.statusCode() + " " + response.bodyAsString();
          logger.warn(errorMsg);
          cf.completeExceptionally(new RuntimeException(errorMsg));
        }
      } else {
        logger.warn(ar.cause());
        cf.completeExceptionally(ar.cause());
      }
    });
    return cf;
  }
}
