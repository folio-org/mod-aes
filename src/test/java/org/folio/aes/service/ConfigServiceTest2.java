package org.folio.aes.service;

import static org.folio.aes.util.Constant.CONFIG_ROUTING_QUREY;

import org.junit.Test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class ConfigServiceTest2 {


  @Test
  public void testConfig() throws InterruptedException {
    Vertx vertx = Vertx.vertx();
    String tenant = "diku";
    String url = "http://localhost:9130" + CONFIG_ROUTING_QUREY;
    String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjgwZDIwMjZkLThjODMtNWMxOC1iYjEzLTlhZDNmNjExMDAzMSIsImlhdCI6MTU0MzQ0NTkzMCwidGVuYW50IjoiZGlrdSJ9.iByzwKA9nUPD40TOJToh8ZzjsgVNn7m1JHM_CpTnFEE";

    Future<Void> future = Future.future();
    WebClient client = WebClient.create(vertx);
    client
    .get(9130, "localhost", CONFIG_ROUTING_QUREY)
    .send(ar -> {
      if (ar.succeeded()) {
        // Obtain response
        HttpResponse<Buffer> response = ar.result();

        System.out.println("Received response with status code" + response.statusCode());
      } else {
        System.out.println("Something went wrong " + ar.cause().getMessage());
      }
      future.complete();

    });

    while (!future.isComplete()) {
      Thread.sleep(1000);
      System.out.println("Waitting");
    }
  }

}
