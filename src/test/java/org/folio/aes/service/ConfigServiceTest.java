package org.folio.aes.service;

import static org.folio.aes.util.Constant.CONFIG_ROUTING_QUREY;

import org.junit.Test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;

public class ConfigServiceTest {


  @Test
  public void testConfig() throws InterruptedException {
    Vertx vertx = Vertx.vertx();
    ConfigService configService = new ConfigService(vertx);
    String tenant = "diku";
    String url = "http://localhost:9130" + CONFIG_ROUTING_QUREY;
    String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjgwZDIwMjZkLThjODMtNWMxOC1iYjEzLTlhZDNmNjExMDAzMSIsImlhdCI6MTU0MzQ0NTkzMCwidGVuYW50IjoiZGlrdSJ9.iByzwKA9nUPD40TOJToh8ZzjsgVNn7m1JHM_CpTnFEE";

    System.out.println(url);
    Future<Void> future = Future.future();
    HttpClientRequest request = vertx.createHttpClient().getAbs(url, response -> {
      response.exceptionHandler(ah -> {
        System.out.println(ah.getMessage());
      });
      System.out.println(response.statusCode());
      future.complete();
    });

    request.exceptionHandler(ah -> {
      System.out.println(ah.getMessage());
    });

    request.end();

    // Future<Void> future = Future.future();
    // configService.getConfig(tenant, token, url, ar -> {
    // if (ar.succeeded()) {
    // System.out.println(ar.result());
    // future.complete();
    // } else {
    // System.out.println(ar.cause());
    // future.complete();
    // }
    // });
    //
    while (!future.isComplete()) {
      Thread.sleep(1000);
      System.out.println("Waitting");
    }
  }

}
