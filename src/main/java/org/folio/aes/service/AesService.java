package org.folio.aes.service;

import static org.folio.aes.util.Constant.*;

import java.util.ArrayList;
import java.util.List;

import org.folio.aes.util.AesUtil;
import org.folio.aes.util.Constant;

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
      kafkaService = new KafkaService(this.vertx, kafkaUrl);
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

        String msg = data.encodePrettily();
        // always send one copy to default tenant topic
        kafkaService.send(kafkaUrl, topic, msg);
        // send to specific topic selectively
        // TODO: assuming some routing rules from configuration
        List<JsonObject> list = new ArrayList<>();
        List<String> jsonPaths = new ArrayList<>();
        for (JsonObject jo : list) {
          jsonPaths.add(jo.getString(Constant.ROUTING_CRITERIA));
        }
        List<Boolean> rs = AesUtil.hasJsonPath(data.toString(), jsonPaths);
        for (int i = 0; i < rs.size(); i++) {
          if (rs.get(i)) {
            kafkaService.send(kafkaUrl, topic + "_" + list.get(i).getString(Constant.ROUTING_TARGET), msg);
          }
        }
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