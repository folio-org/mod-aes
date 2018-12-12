package org.folio.aes.service;

import static org.folio.aes.util.AesConstants.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.folio.aes.model.RoutingRule;
import org.folio.aes.util.AesUtils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class AesService {

  private static Logger logger = LoggerFactory.getLogger(AesService.class);

  private Vertx vertx;
  private RuleConfigService ruleConfigService;
  private QueueService queueService;

  // prevent circular filter call
  private ConcurrentMap<String, Long> aesFilterIds = new ConcurrentHashMap<>();

  public AesService(Vertx vertx, String kafkaUrl) {
    this.vertx = vertx;
    ruleConfigService = new RuleConfigService(this.vertx);
    if (kafkaUrl == null) {
      queueService = new LogQueueService();
    } else {
      queueService = new KafkaQueueService(this.vertx, kafkaUrl);
    }
  }

  public void stop(Handler<AsyncResult<Void>> resHandler) {
    queueService.stop();
  }

  public void prePostHandler(RoutingContext ctx) {
    MultiMap headers = ctx.request().headers();
    // skip self-calling
    if (headers.contains(AES_FILTER_ID)) {
      ctx.response().end();
      return;
    }

    JsonObject data = collectData(ctx);
    String msg = data.encodePrettily();
    logger.trace(msg);

    String tenant = headers.get(OKAPI_TENANT);
    String token = headers.get(OKAPI_TOKEN);
    String url = headers.get(OKAPI_URL) + CONFIG_ROUTING_QUREY;

    Future<Void> future = Future.future();
    if (tenant == null) {
      // always send a copy for no-tenant request
      queueService.send(TENANT_NONE, msg);
    } else {
      String filterId = UUID.randomUUID().toString();
      aesFilterIds.put(filterId, System.currentTimeMillis());
      ruleConfigService.getConfig(tenant, token, url, filterId, handler -> {
        if (handler.succeeded()) {
          List<RoutingRule> rules = handler.result();
          Set<String> jsonPaths = new HashSet<>();
          rules.forEach(r -> jsonPaths.add(r.getCriteria()));
          Set<String> validJsonPaths = AesUtils.checkJsonPaths(msg, jsonPaths);
          rules.forEach(r -> {
            if (validJsonPaths.contains(r.getCriteria())) {
              queueService.send(r.getTarget(), msg);
            }
          });
        } else {
          logger.error(handler.cause());
        }
        aesFilterIds.remove(filterId);
      });
    }
    ctx.response().end();
  }

  private JsonObject collectData(RoutingContext ctx) {
    JsonObject data = new JsonObject();
    data.put("path", ctx.normalisedPath());
    data.put("headers", AesUtils.convertMultiMapToJsonObject(ctx.request().headers()));
    data.put("params", AesUtils.convertMultiMapToJsonObject(ctx.request().params()));
    String bodyString = ctx.getBodyAsString();
    data.put("pii", AesUtil.containsPII(bodyString));
    if (bodyString.length() > BODY_LIMIT) {
      data.put("body", new JsonObject().put("content",
        bodyString.substring(0, BODY_LIMIT) + "..."));
    } else {
      try {
        JsonObject bodyJsonObject = new JsonObject(bodyString);
        AesUtils.maskPassword(bodyJsonObject);
        data.put("body", bodyJsonObject);
      } catch (Exception e) {
        data.put("body", new JsonObject().put("content", bodyString));
      }
    }
    return data;
  }

}