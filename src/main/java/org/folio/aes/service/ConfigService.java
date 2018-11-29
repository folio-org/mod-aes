package org.folio.aes.service;

import static org.folio.aes.util.Constant.OKAPI_TENANT;
import static org.folio.aes.util.Constant.OKAPI_TOKEN;

import java.util.ArrayList;
import java.util.List;

import org.folio.aes.model.RoutingRule;
import org.folio.aes.util.Constant;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ConfigService {

  private static Logger logger = LoggerFactory.getLogger(ConfigService.class);

  private Vertx vertx;

  public ConfigService(Vertx vertx) {
    this.vertx = vertx;
  }

  public void getConfig(String tenant, String token, String url, Handler<AsyncResult<List<RoutingRule>>> handler) {
    System.out.println("HJI debug");
    System.out.println(tenant);
    System.out.println(token);
    System.out.println(url);

    HttpClient client = vertx.createHttpClient();
    HttpClientRequest request = client.getAbs(url, response -> {
      response.exceptionHandler(th -> {
        logger.error(th);
        handler.handle(Future.failedFuture(th));
      });
      System.out.println(response.statusCode());
      if (response.statusCode() == 200) {
        response.bodyHandler(b -> {
          System.out.println(b);
          List<RoutingRule> rules = new ArrayList<>();
          JsonArray ja = new JsonObject(b).getJsonArray("configs");
          ja.forEach(item -> {
            JsonObject jo = (JsonObject) item;
            rules.add(new RoutingRule(jo.getString(Constant.ROUTING_CRITERIA), jo.getString(Constant.ROUTING_TARGET)));
          });
          handler.handle(Future.succeededFuture(rules));
        });
      } else {
        logger.error("Failed to retrieve configuration records. Status code: " + response.statusCode());
        response.bodyHandler(b -> {
          logger.error(b.toString());
        });
        handler.handle(Future.failedFuture("Failed to retrieve configuration records"));
      }
    });
    request.exceptionHandler(th -> {
      logger.error(th);
      handler.handle(Future.failedFuture(th));
    });
    if (tenant != null) {
      request.putHeader(OKAPI_TENANT, tenant);
    }
    if (token != null) {
      request.putHeader(OKAPI_TOKEN, token);
    }
    request.putHeader("Accept", "application/json");
    request.end();
  }

}
