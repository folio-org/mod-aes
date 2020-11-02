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
import io.vertx.core.MultiMap;
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
  private ConcurrentMap<String, CachedRoutingRules> cachedRules = new ConcurrentHashMap<>();

  public RuleServiceConfigImpl(Vertx vertx) {
    this.client = WebClient.create(vertx);
  }

  private static class CachedRoutingRules {
    private long timestamp = 0L;
    private Collection<RoutingRule> rules = new ArrayList<>();

    public CachedRoutingRules(long timestamp, Collection<RoutingRule> rules) {
      this.timestamp = timestamp;
      this.rules = rules;
    }
  }

  @Override
  public Future<Collection<RoutingRule>> getRules(String okapiUrl, String tenant, String token) {
    final Promise <Collection<RoutingRule>> promise = Promise.promise();
    CachedRoutingRules cache = cachedRules.get(tenant);
    if (cache != null && (System.currentTimeMillis() - cache.timestamp < CACHE_PERIOD)) {
      promise.complete(cache.rules);
    } else {
      logger.info("Refresh routing rules for tenant {}", tenant);
      getFreshRules(okapiUrl, tenant, token)
        .map(rules -> {
          cachedRules.put(tenant, new CachedRoutingRules(System.currentTimeMillis(), rules));
          promise.complete(rules);
          logger.info("Refreshed rules: {}", rules);
          return rules;
        })
        .otherwise(ex -> {
          promise.fail(ex);
          if (ex != null) {
            logger.warn(ex);
          }
          return null;
        });
    }
    return promise.future();
  }

  @Override
  public void stop() {
    client.close();
  }

  private Future<Collection<RoutingRule>> getFreshRules(String okapiUrl, String tenant, String token) {
    final Promise<Collection<RoutingRule>> promise = Promise.promise();
    HttpRequest<Buffer> request = client.getAbs(okapiUrl);
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
          promise.complete(rules);
        } else {
          String errorMsg = response.statusCode() + " " + response.bodyAsString();
          logger.warn(errorMsg);
          promise.fail(new RuntimeException(errorMsg));
        }
      } else {
        logger.warn(ar.cause());
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }
}
