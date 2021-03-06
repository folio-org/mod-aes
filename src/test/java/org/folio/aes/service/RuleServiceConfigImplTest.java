package org.folio.aes.service;

import static org.folio.aes.test.Utils.nextFreePort;
import static org.folio.aes.util.AesConstants.CONFIG_CONFIGS;
import static org.folio.aes.util.AesConstants.CONFIG_ROUTING_CRITERIA;
import static org.folio.aes.util.AesConstants.CONFIG_ROUTING_QUERY;
import static org.folio.aes.util.AesConstants.CONFIG_ROUTING_TARGET;
import static org.folio.aes.util.AesConstants.CONFIG_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Collection;
import org.folio.aes.model.RoutingRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class RuleServiceConfigImplTest {

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
      if (CONFIG_ROUTING_QUERY.startsWith(r.path())) {
        r.response().end(getConfigMock());
      } else {
        r.response().setStatusCode(404).end();
      }
    }).listen(port, context.succeedingThenComplete());
  }

  @AfterAll
  @DisplayName("Shut down HTTP server")
  public static void tearDownOnce(Vertx vertx, VertxTestContext context) {
    vertx.close(context.succeedingThenComplete());
  }

  @BeforeEach
  public void setUp(Vertx vertx) {
    ruleService = new RuleServiceConfigImpl(vertx);
  }

  @Test
  void testGetConfig(VertxTestContext testContext) throws Exception {
    String okapiUrl = "http://localhost:" + port + CONFIG_ROUTING_QUERY;
    Future<Collection<RoutingRule>> f1 = ruleService.getRules(okapiUrl, tenant, token);
    f1.onComplete(testContext.succeeding(v -> {
      assertEquals(count, f1.result().size());
      Future<Collection<RoutingRule>> f2 = ruleService.getRules(okapiUrl, tenant, token);
      f2.onComplete(testContext.succeeding(v1 -> {
        assertEquals(count, f2.result().size());
        testContext.completeNow();
      }));
    }));
  }

  @Test
  void testGetConfigFailure(VertxTestContext testContext) throws Exception {
    String okapiUrl = "http://localhost:" + port + "/xyzzy";
    Future<Collection<RoutingRule>> f1 = ruleService.getRules(okapiUrl, tenant, token);
    f1.onComplete(testContext.failing(v -> {
      assertEquals("404 null", f1.cause().getMessage());
      testContext.completeNow();
    }));
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
}
