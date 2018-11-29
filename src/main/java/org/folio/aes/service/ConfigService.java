package org.folio.aes.service;

import static org.folio.aes.util.Constant.*;

import java.util.ArrayList;
import java.util.List;

import org.folio.aes.model.RoutingRule;
import org.folio.aes.util.Constant;

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

public class ConfigService {

  private static Logger logger = LoggerFactory.getLogger(ConfigService.class);

  private Vertx vertx;

  public ConfigService(Vertx vertx) {
    this.vertx = vertx;
  }

  public void getConfig(String tenant, String token, String url, Handler<AsyncResult<List<RoutingRule>>> handler) {

    HttpRequest<Buffer> request = WebClient.create(vertx).getAbs(url);
    MultiMap headers = request.headers();
    headers.set("Accept", "application/json");
    if (tenant != null) {
      headers.set(Constant.OKAPI_TENANT, tenant);
    }
    if (token != null) {
      headers.set(Constant.OKAPI_TOKEN, token);
    }

    request.send(ar -> {
      if (ar.succeeded()) {
        HttpResponse<Buffer> response = ar.result();
        if (response.statusCode() == 200) {
          List<RoutingRule> rules = new ArrayList<>();
          response.bodyAsJsonObject().getJsonArray("configs").forEach(item -> {
            JsonObject jo = (JsonObject) item;
            rules.add(new RoutingRule(jo.getString(ROUTING_CRITERIA), jo.getString(ROUTING_TARGET)));
          });
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
