package org.folio.aes.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.folio.aes.util.AesConstants.CONFIG_CONFIGS;
import static org.folio.aes.util.AesConstants.CONFIG_ROUTING_CRITERIA;
import static org.folio.aes.util.AesConstants.CONFIG_ROUTING_TARGET;
import static org.folio.aes.util.AesConstants.CONFIG_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.folio.aes.model.RoutingRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class RuleServiceConfigImplTest {

  private static int port;
  private static String tenant = "tenant";
  private static String token = "token";
  private static int count = 10;

  private RuleService ruleService;

  @BeforeAll
  @DisplayName("Start HTTP server")
  public static void setUpOnce(Vertx vertx, VertxTestContext context) throws Throwable {
    port = nextFreePort();
    vertx.createHttpServer().requestHandler(r -> {
      r.response().end(getConfigMock());
    }).listen(port, context.completing());

    assertTrue(context.awaitCompletion(5, SECONDS));

    if (context.failed()) {
      throw context.causeOfFailure();
    }
  }

  @AfterAll
  @DisplayName("Shut down HTTP server")
  public static void tearDownOnce(Vertx vertx, VertxTestContext context) {
    vertx.close(context.completing());
  }

  @BeforeEach
  public void setUp(Vertx vertx) {
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
