package org.folio.aes.service;

import static org.folio.aes.util.AesConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.folio.aes.model.RoutingRule;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
 * Retrieve and cache {@link RoutingRule}s.
 */
public class RuleConfigService {

  private static Logger logger = LoggerFactory.getLogger(RuleConfigService.class);

  private Vertx vertx;

  public RuleConfigService(Vertx vertx) {
    this.vertx = vertx;
  }

  private static class CachedRules {
    public long timestamp = 0L;
    public List<RoutingRule> rules = new ArrayList<>();

    public CachedRules(long timestamp, List<RoutingRule> rules) {
      this.timestamp = timestamp;
      this.rules = rules;
    }
  }

  private static final long cachePeriod = 1 * 60 * 1000; // one minute
  private ConcurrentMap<String, CachedRules> cache = new ConcurrentHashMap<>();

  public CompletableFuture<List<RoutingRule>> getConfig(String tenant, String token, String url, String filterId) {
    CompletableFuture<List<RoutingRule>> cf = new CompletableFuture<>();
    CachedRules cr = cache.get(tenant);
    if (cr != null && (System.currentTimeMillis() - cr.timestamp < cachePeriod)) {
      cf.complete(cr.rules);
    } else {
      logger.info("Refresh routing rules for tenant " + tenant);
//      CompletableFuture.supplyAsync(() -> refreshConfig(tenant, token, url, filterId)).thenAccept(rules -> {
//        cache.put(tenant, new CachedRules(System.currentTimeMillis(), rules));
//        cf.complete(rules);
//        logger.info("Refreshed rules: " + rules);
//      });

    }
    return cf;
  }

  private void refreshConfig(String tenant, String token, String url, String filterId) {
    logger.info("Refresh routing rules for tenant " + tenant);

    HttpRequest<Buffer> request = WebClient.create(vertx).getAbs(url);
    MultiMap headers = request.headers();
    headers.set("Accept", "application/json");
    headers.set(AES_FILTER_ID, filterId);
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
          List<RoutingRule> rules = new ArrayList<>();
          response.bodyAsJsonObject().getJsonArray("configs").forEach(item -> {
            JsonObject jo = new JsonObject(((JsonObject) item).getString("value"));
            String topic = String.format("TENANT_%s_%s", tenant, jo.getString(ROUTING_TARGET));
            rules.add(new RoutingRule(jo.getString(ROUTING_CRITERIA), topic));
          });
//          return rules;
        } else {
          String errorMsg = response.statusCode() + " " + response.bodyAsString();
          logger.warn(errorMsg);
          throw new RuntimeException(errorMsg);
        }
      } else {
        logger.warn(ar.cause());
        throw new RuntimeException(ar.cause());
      }
    });

  }

  public void getConfig(String tenant, String token, String url, String filterId,
      Handler<AsyncResult<List<RoutingRule>>> handler) {

    CachedRules cr = cache.get(tenant);
    if (cr != null && (System.currentTimeMillis() - cr.timestamp < cachePeriod)) {
      handler.handle(Future.succeededFuture(cr.rules));
      return;
    }

    logger.info("Refresh routing rules for tenant " + tenant);

    HttpRequest<Buffer> request = WebClient.create(vertx).getAbs(url);
    MultiMap headers = request.headers();
    headers.set("Accept", "application/json");
    headers.set(AES_FILTER_ID, filterId);
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
          List<RoutingRule> rules = new ArrayList<>();
          response.bodyAsJsonObject().getJsonArray("configs").forEach(item -> {
            JsonObject jo = new JsonObject(((JsonObject) item).getString("value"));
            String topic = String.format("TENANT_%s_%s", tenant, jo.getString(ROUTING_TARGET));
            rules.add(new RoutingRule(jo.getString(ROUTING_CRITERIA), topic));
          });
          cache.put(tenant, new CachedRules(System.currentTimeMillis(), rules));
          logger.info("Refreshed rules: " + rules);
          handler.handle(Future.succeededFuture(rules));
        } else {
          String errorMsg = response.statusCode() + " " + response.bodyAsString();
          logger.warn(errorMsg);
          handler.handle(Future.failedFuture(errorMsg));
        }
      } else {
        logger.warn(ar.cause());
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });

  }

}
