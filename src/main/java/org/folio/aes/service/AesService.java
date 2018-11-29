package org.folio.aes.service;

import static org.folio.aes.util.Constant.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.folio.aes.model.RoutingRule;
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

  // private KafkaService kafkaService;
  private DummyQueueService kafkaService = new DummyQueueService();

  private ConfigService configService;

  public AesService(Vertx vertx, String kafkaUrl) {
    this.vertx = vertx;
    this.kafkaUrl = kafkaUrl;
    configService = new ConfigService(this.vertx);
    if (kafkaUrl != null && !kafkaUrl.isEmpty()) {
      logger.info("Initialize KafkaService");
      // kafkaService = new KafkaService(this.vertx, kafkaUrl);
    }
  }

  public void prePostHandler(RoutingContext ctx) {
    String phase = "" + ctx.request().headers().get(OKAPI_FILTER);
    if (phase.startsWith(PHASE_PRE) || phase.startsWith(PHASE_POST)) {
      JsonObject data = collectData(ctx);
      String msg = data.encodePrettily();
      logger.debug(msg);
      if (kafkaService != null) {
        String tenant = ctx.request().headers().get(OKAPI_TENANT);
        String token = ctx.request().headers().get(OKAPI_TOKEN);
        String url = ctx.request().headers().get(OKAPI_URL) + CONFIG_ROUTING_QUREY;
        url = "http://localhost:9130" + CONFIG_ROUTING_QUREY;
        token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjgwZDIwMjZkLThjODMtNWMxOC1iYjEzLTlhZDNmNjExMDAzMSIsImlhdCI6MTU0MzQ0NTkzMCwidGVuYW50IjoiZGlrdSJ9.iByzwKA9nUPD40TOJToh8ZzjsgVNn7m1JHM_CpTnFEE";

        if (tenant == null) {
          kafkaService.send(kafkaUrl, TENANT_DEFAULT, msg);
        } else {
          kafkaService.send(kafkaUrl, tenant, msg);
          configService.getConfig(tenant, token, url, handler -> {
            if (handler.succeeded()) {
              List<RoutingRule> rules = handler.result();
              System.out.println("Got rules: " + rules);
              Set<String> jsonPaths = new HashSet<>();
              rules.forEach(r -> jsonPaths.add(r.getCriteria()));
              Set<String> rs = AesUtil.checkJsonPaths(msg, jsonPaths);
              rules.forEach(r -> {
                if (rs.contains(r.getCriteria())) {
                  kafkaService.send(kafkaUrl, tenant + "_" + r.getTarget(), msg);
                }
              });
            } else {
              logger.error(handler.cause());
            }
          });
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