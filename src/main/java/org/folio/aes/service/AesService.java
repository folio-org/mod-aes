package org.folio.aes.service;

import static org.folio.aes.util.Constant.*;

import org.folio.aes.util.AesUtil;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class AesService {

  private static Logger logger = LoggerFactory.getLogger(AesService.class);

  private Vertx vertx;

  private String kafkaUrl;

  private KafkaService kafkaService;

  public AesService(Vertx vertx, String kafkaUrl) {
    this.vertx = vertx;
    this.kafkaUrl = kafkaUrl;
    if (kafkaUrl != null && !kafkaUrl.isEmpty()) {
      logger.info("Initialize KafkaService");
      kafkaService = new KafkaService(this.vertx);
    }
  }

  public void prePostHandler(RoutingContext ctx) {
    String phase = "" + ctx.request().headers().get(OKAPI_FILTER);
    if (phase.startsWith(PHASE_PRE) || phase.startsWith(PHASE_POST)) {
      JsonObject data = collectData(ctx);
      logger.debug(data.encodePrettily());
      if (kafkaService != null) {
        String topic = ctx.request().headers().get(OKAPI_TENANT);
        topic = topic == null ? TENANT_DEFAULT : topic;
        kafkaService.send(kafkaUrl, topic, data.encodePrettily());
      }
    }
    ctx.response().end();
  }

  private JsonObject collectData(RoutingContext ctx) {
    JsonObject data = new JsonObject();
    data.put("path", ctx.normalisedPath());
    data.put("headers", AesUtil.convertMultiMapToJsonObject(ctx.request().headers()));
    data.put("params", AesUtil.convertMultiMapToJsonObject(ctx.request().params()));
    String bodyString = ctx.getBodyAsString();
    data.put("pii", AesUtil.containsPII(bodyString));
    if (bodyString.length() > BODY_LIMIT) {
      data.put("body", new JsonObject().put("content",
        bodyString.substring(0, BODY_LIMIT) + "..."));
    } else {
      try {
        JsonObject bodyJsonObject = new JsonObject(bodyString);
        AesUtil.maskPassword(bodyJsonObject);
        data.put("body", bodyJsonObject);
      } catch (Exception e) {
        data.put("body", new JsonObject().put("content", bodyString));
      }
    }
    return data;
  }

}