package org.folio.aes;

import static org.folio.aes.test.Utils.nextFreePort;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class AesVerticleTest {
  private static Logger log = LogManager.getLogger();

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name",
        "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  @BeforeEach
  @DisplayName("Deploy the verticle")
  void setUp(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    log.info("Starting: {}", testInfo.getDisplayName());

    final int port = nextFreePort();
    final JsonObject config = new JsonObject();
    config.put("port", port);

    final DeploymentOptions opts = new DeploymentOptions();
    opts.setConfig(config);

    vertx.deployVerticle(new AesVerticle(), opts, testContext.completing());

    RestAssured.port = port;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    log.info("Verticle deployment complete");
  }

  @AfterEach
  @DisplayName("Shutdown")
  void tearDown(Vertx vertx, TestInfo testInfo) {
    log.info("Finished: {}", testInfo.getDisplayName());
  }

  @Test
  void rootApiResponds() {
    RestAssured
      .given()
      .when()
        .get("/")
      .then()
        .statusCode(is(200))
        .body(is("mod-aes is up running"));
  }

  @Test
  void healthCheckResponds() {
    RestAssured
      .given()
      .when()
        .get("/admin/health")
      .then()
        .statusCode(is(200))
        .body(is("OK"));
  }
}
