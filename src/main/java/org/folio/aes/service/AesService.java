package org.folio.aes.service;

import static org.folio.aes.util.AesConstants.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.folio.aes.util.AesUtils;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class AesService {

  private static Logger logger = LoggerFactory.getLogger(AesService.class);

  private RuleService ruleService;
  private QueueService queueService;

  public CompletableFuture<Void> stop() {
    return getQueueService().stop();
  }

  public void prePostHandler(RoutingContext ctx) {
    MultiMap headers = ctx.request().headers();
    boolean skip = false;
    // need a better way to skip self-calling
    if (headers.contains(OKAPI_AES_FILTER_ID)) {
      skip = true;
    }
    // skip FOLIO internal AUTH checking
    String token = headers.get(OKAPI_TOKEN);
    if (!skip && token != null) {
      JsonObject jo = new JsonObject();
      try {
        jo = AesUtils.decodeOkapiToken(token);
        if (OKAPI_TOKEN_AUTH_MOD.equals(jo.getString(OKAPI_TOKEN_SUB))) {
          skip = true;
        }
      } catch (Exception e) {
        logger.warn("Invalid Okapi token format: " + token);
        skip = true;
      }
    }
    if (skip) {
      ctx.response().end();
      return;
    }

    JsonObject data = collectData(ctx);
    String msg = data.encodePrettily();
    logger.trace(msg);

    String okapiUrl = headers.get(OKAPI_URL) + CONFIG_ROUTING_QUREY;
    String tenant = headers.get(OKAPI_TENANT);

    // Run it asynchronously since OKAPI does not care response
    CompletableFuture.runAsync(() -> {
      if (tenant == null) {
        // edge case: always send a copy for no-tenant request
        getQueueService().send(TENANT_NONE, msg);
      } else {
        getRuleService().getRules(okapiUrl, tenant, token).thenAccept(rules -> {
          if (rules.isEmpty()) {
            // send all messages to default topic if no rules defined
            getQueueService().send(tenant + "_default", msg);
          } else {
            Set<String> jsonPaths = new HashSet<>();
            rules.forEach(r -> jsonPaths.add(r.getCriteria()));
            Set<String> validJsonPaths = AesUtils.checkJsonPaths(msg, jsonPaths);
            rules.forEach(r -> {
              if (validJsonPaths.contains(r.getCriteria())) {
                getQueueService().send(r.getTarget(), msg);
              }
            });
          }
        }).handle((res, ex) -> {
          if (ex != null) {
            logger.warn(ex);
            return ex;
          } else {
            return res;
          }
        });
      }
    });

    ctx.response().end();
  }

  private JsonObject collectData(RoutingContext ctx) {
    JsonObject data = new JsonObject();
    data.put(MSG_PATH, ctx.normalisedPath());
    data.put(MSG_HEADERS, AesUtils.convertMultiMapToJsonObject(ctx.request().headers()));
    data.put(MSG_PARAMS, AesUtils.convertMultiMapToJsonObject(ctx.request().params()));
    String bodyString = ctx.getBodyAsString();
    JsonObject bodyJsonObject = null;
    if (ctx.request().headers().get("Content-Type").toLowerCase().contains("json")) {
      try {
        bodyJsonObject = new JsonObject(bodyString);
      } catch (Exception e) {
        logger.warn("Failed to convert to JSON: " + ctx.getBodyAsString());
        bodyJsonObject = null;
      }
    }
    if (bodyJsonObject != null) {
      AesUtils.maskPassword(bodyJsonObject);
      data.put(MSG_PII, AesUtils.containsPII(bodyString));
      data.put(MSG_BODY, bodyJsonObject);
    } else {
      data.put(MSG_PII, false);
      data.put(MSG_BODY, new JsonObject().put(MSG_BODY_CONTENT, bodyString));
    }
    return data;
  }

  public RuleService getRuleService() {
    return ruleService;
  }

  public void setRuleService(RuleService ruleService) {
    this.ruleService = ruleService;
  }

  public QueueService getQueueService() {
    return queueService;
  }

  public void setQueueService(QueueService queueService) {
    this.queueService = queueService;
  }

}