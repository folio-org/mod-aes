package org.folio.aes.service;

import static org.folio.aes.util.AesConstants.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.folio.aes.model.RoutingRule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class RuleServiceConfigImplTest {

  private static int port;
  private static Vertx vertx;
  private static HttpServer okapi;
  private static String tenant = "tenant";
  private static String token = "token";
  private static int count = 10;

  private RuleService ruleService;

  @BeforeClass
  public static void setUpOnce(TestContext context) throws Exception {
    Async async = context.async();
    port = nextFreePort();
    vertx = Vertx.vertx();
    okapi = vertx.createHttpServer().requestHandler(r -> {
      r.response().end(getConfigMock());
    }).listen(port, result -> {
      context.assertTrue(result.succeeded());
      async.complete();
    });
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) throws Exception {
    Async async = context.async();
    okapi.close(r -> async.complete());
  }

  @Before
  public void setUp() {
    ruleService = new RuleServiceConfigImpl(vertx);
  }

  @Test
  public void testGetConfig() throws Exception {
    String okapiUrl = "http://localhost:" + port;
    CompletableFuture<Collection<RoutingRule>> cf = ruleService.getRules(okapiUrl, tenant, token);
    Collection<RoutingRule> rules = cf.get();
    assertEquals(count, rules.size());
    cf = ruleService.getRules(okapiUrl, tenant, token);
    rules = cf.get();
    assertEquals(count, rules.size());
  }

  private static String getConfigMock() {
    JsonObject jo = new JsonObject();
    JsonArray ja = new JsonArray();
    for (int i = 0; i < count; i++) {
      JsonObject val = new JsonObject();
      JsonObject rule = new JsonObject();
      rule.put(CONFIG_ROUTING_TARGET, "t" + i);
      rule.put(CONFIG_ROUTING_CRITERIA, "c" + i);
      val.put(CONFIG_VALUE, rule.encode());
      ja.add(val);
    }
    jo.put(CONFIG_CONFIGS, ja);
    return jo.encodePrettily();
  }

  private static int nextFreePort() {
    int maxTries = 10000;
    int port = ThreadLocalRandom.current().nextInt(49152, 65535);
    while (true) {
      if (isLocalPortFree(port)) {
        return port;
      } else {
        port = ThreadLocalRandom.current().nextInt(49152, 65535);
      }
      maxTries--;
      if (maxTries == 0) {
        return 8081;
      }
    }
  }

  private static boolean isLocalPortFree(int port) {
    try {
      new ServerSocket(port).close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
