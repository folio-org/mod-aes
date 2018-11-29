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

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class AesService {

  private static Logger logger = LoggerFactory.getLogger(AesService.class);

  private ConfigService configService;
  private QueueService queueService;

  // prevent circular filter call
  private ConcurrentMap<String, Long> aesFilterIds = new ConcurrentHashMap<>();

  public AesService(ConfigService configService, QueueService queueService) {
    this.configService = configService;
    this.queueService = queueService;
  }

  public void prePostHandler(RoutingContext ctx) {
    MultiMap headers = ctx.request().headers();
    if (headers.contains(AES_FILTER_ID)) {
      aesFilterIds.remove(headers.get(AES_FILTER_ID));
      ctx.response().end();
      return;
    }

    String phase = "" + headers.get(OKAPI_FILTER);
    if (phase.startsWith(PHASE_PRE) || phase.startsWith(PHASE_POST)) {
      JsonObject data = collectData(ctx);
      String msg = data.encodePrettily();
      logger.debug(msg);

      String tenant = ctx.request().headers().get(OKAPI_TENANT);
      String token = ctx.request().headers().get(OKAPI_TOKEN);
      String url = ctx.request().headers().get(OKAPI_URL) + CONFIG_ROUTING_QUREY;
      // TODO: temp testing url
      url = "http://localhost:9130" + CONFIG_ROUTING_QUREY;

      if (tenant == null) {
        queueService.send(TENANT_DEFAULT, msg);
      } else {
        queueService.send(tenant, msg);
        String filterId = UUID.randomUUID().toString();
        aesFilterIds.put(filterId, System.currentTimeMillis());
        configService.getConfig(tenant, token, url, filterId, handler -> {
          if (handler.succeeded()) {
            List<RoutingRule> rules = handler.result();
            System.out.println("Got rules: " + rules);
            Set<String> jsonPaths = new HashSet<>();
            rules.forEach(r -> jsonPaths.add(r.getCriteria()));
            Set<String> validJsonPaths = AesUtils.checkJsonPaths(msg, jsonPaths);
            rules.forEach(r -> {
              if (validJsonPaths.contains(r.getCriteria())) {
                queueService.send(tenant + "_" + r.getTarget(), msg);
              }
            });
          } else {
            logger.error(handler.cause());
          }
          aesFilterIds.remove(filterId);
        });
      }
    }

    ctx.response().end();
  }

  private JsonObject collectData(RoutingContext ctx) {
    JsonObject data = new JsonObject();
    data.put("path", ctx.normalisedPath());
    data.put("headers", AesUtils.convertMultiMapToJsonObject(ctx.request().headers()));
    data.put("params", AesUtils.convertMultiMapToJsonObject(ctx.request().params()));
    String bodyString = ctx.getBodyAsString();
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