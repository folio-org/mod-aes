package org.folio.aes.service;

import static org.folio.aes.util.AesConstants.CONFIG_ROUTING_QUERY;
import static org.folio.aes.util.AesConstants.CONTENT_TYPE;
import static org.folio.aes.util.AesConstants.MSG_BODY;
import static org.folio.aes.util.AesConstants.MSG_BODY_CONTENT;
import static org.folio.aes.util.AesConstants.MSG_HEADERS;
import static org.folio.aes.util.AesConstants.MSG_PARAMS;
import static org.folio.aes.util.AesConstants.MSG_PATH;
import static org.folio.aes.util.AesConstants.MSG_PII;
import static org.folio.aes.util.AesConstants.OKAPI_AES_FILTER_ID;
import static org.folio.aes.util.AesConstants.OKAPI_HANDLER_RESULT;
import static org.folio.aes.util.AesConstants.OKAPI_TENANT;
import static org.folio.aes.util.AesConstants.OKAPI_TOKEN;
import static org.folio.aes.util.AesConstants.OKAPI_TOKEN_AUTH_MOD;
import static org.folio.aes.util.AesConstants.OKAPI_TOKEN_SUB;
import static org.folio.aes.util.AesConstants.OKAPI_URL;
import static org.folio.aes.util.AesConstants.TENANT_NONE;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.aes.util.AesUtils;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class AesService {

  private static Logger logger = LogManager.getLogger();

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
        logger.warn("Invalid Okapi token format: {}", token);
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

    String okapiUrl = headers.get(OKAPI_URL) + CONFIG_ROUTING_QUERY;
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
    final JsonObject data = new JsonObject();
    data.put(MSG_PATH, ctx.normalisedPath());
    data.put(MSG_HEADERS, AesUtils.convertMultiMapToJsonObject(ctx.request().headers()));
    data.put(MSG_PARAMS, AesUtils.convertMultiMapToJsonObject(ctx.request().params()));
    final Buffer bodyBuffer = ctx.getBody() == null ? Buffer.buffer() : ctx.getBody();
    // Get the actual response status code from this Okapi provided header
    final String statusCodeString = ctx.request().headers().get(OKAPI_HANDLER_RESULT);
    final boolean success = statusCodeString == null
        || Integer.parseInt(statusCodeString) == 422 // this error should be JSON formatted
        || (Integer.parseInt(statusCodeString) >= 200 && Integer.parseInt(statusCodeString) < 300);
    final String contentType = ctx.request().headers().get(CONTENT_TYPE);
    Object bodyJson = null;
    if (success && bodyBuffer.length() > 0 && contentType != null &&
        contentType.toLowerCase().contains("json")) {
      try {
        bodyJson = bodyBuffer.toJson();
      } catch (Exception e) {
        logger.warn("Failed to convert to JSON: {}", bodyBuffer::toString);
        bodyJson = null;
      }
    }
    if (bodyJson != null) {
      AesUtils.maskPassword(bodyJson);
      data.put(MSG_PII, AesUtils.containsPII(bodyJson));
      data.put(MSG_BODY, bodyJson);
    } else {
      data.put(MSG_PII, false);
      data.put(MSG_BODY, new JsonObject().put(MSG_BODY_CONTENT, bodyBuffer.toString()));
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