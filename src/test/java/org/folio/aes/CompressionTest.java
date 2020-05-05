package org.folio.aes;

import static io.vertx.core.buffer.Buffer.buffer;
import static io.vertx.junit5.web.TestRequest.bodyResponse;
import static io.vertx.junit5.web.TestRequest.requestHeader;
import static io.vertx.junit5.web.TestRequest.statusCode;
import static io.vertx.junit5.web.TestRequest.testRequest;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.aes.test.Utils.nextFreePort;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.aes.service.AesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.junit5.web.VertxWebClientExtension;
import io.vertx.junit5.web.WebClientOptionsInject;

/**
 * This test is to demonstrate that compressed data passed with 
 * @author mreno
 *
 */
@ExtendWith({VertxExtension.class, VertxWebClientExtension.class, MockitoExtension.class})
class CompressionTest {
  private static Logger log = LogManager.getLogger();

  private static final int PORT = nextFreePort();

  @Mock
  private AesService aesService;

  @InjectMocks
  private AesVerticle aesVerticle;

  @WebClientOptionsInject
  public WebClientOptions options = new WebClientOptions()
    .setDefaultHost("localhost")
    .setDefaultPort(PORT)
    .setTryUseCompression(true);

  private final Buffer compressedJson = buffer(new byte [] {
      (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xaa, (byte) 0x56, (byte) 0xca, (byte) 0x4e,
      (byte) 0xad, (byte) 0x54, (byte) 0xb2, (byte) 0x52, (byte) 0x50, (byte) 0x2a, (byte) 0x49,
      (byte) 0x2d, (byte) 0x2e, (byte) 0x51, (byte) 0xd2, (byte) 0x51, (byte) 0x50, (byte) 0x2a,
      (byte) 0x4b, (byte) 0xcc, (byte) 0x29, (byte) 0x4d, (byte) 0x05, (byte) 0x0b, (byte) 0x64,
      (byte) 0x64, (byte) 0x16, (byte) 0x2b, (byte) 0x00, (byte) 0x51, (byte) 0xa2, (byte) 0x02,
      (byte) 0x58, (byte) 0xaa, (byte) 0x16, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff,
      (byte) 0xff, (byte) 0x03, (byte) 0x00, (byte) 0xf8, (byte) 0x13, (byte) 0x1c, (byte) 0x36,
      (byte) 0x2a, (byte) 0x00, (byte) 0x00, (byte) 0x00
    });

  private final Buffer expectedJson = buffer("{\"key\": \"test\", \"value\": \"this is a test\"}");

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name",
        "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  @BeforeEach
  @DisplayName("Deploy the verticle")
  void setUp(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    log.info("Starting: {}", testInfo.getDisplayName());

    final JsonObject config = new JsonObject();
    config.put("port", PORT);

    final DeploymentOptions opts = new DeploymentOptions();
    opts.setConfig(config);

    vertx.deployVerticle(aesVerticle, opts, testContext.completing());

    log.info("Verticle deployment complete");
}

  @AfterEach
  @DisplayName("Shutdown")
  void tearDown(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) {
    // The AES service is stopped when the verticle is stopped, so we need the AES service mock
    // to return a CompletableFuture so that there won't be an NPE. This also requires manual
    // undeployment instead of letting the Vert.x extension take care of it, since Mockito
    // will think the stop mocking stub is unnecessary as stop is called after Mockito does
    // its stubbing verification. We could use a lenient stub to solve this, but since we
    // explicitly deploy, we may as well explicitly undeploy.
    when(aesService.stop()).thenReturn(completedFuture(null));

    vertx.undeploy(vertx.deploymentIDs().iterator().next(), testContext.completing());

    log.info("Finished: {}", testInfo.getDisplayName());
  }

  @Test
  void canHandleCompressedClientData(Vertx vertx, VertxTestContext testContext, WebClient client) {
    // Mock the AES service so that we just return the body passed in as the response.
    // The routing context will have the decompressed body if server side decompression is
    // enabled.
    doAnswer(invocation -> {
      final RoutingContext ctx = invocation.getArgument(0, RoutingContext.class);

      ctx.response().putHeader("Content-Type", "application/text");
      ctx.response().end(ctx.getBody());

      return null;
    }).when(aesService).prePostHandler(any(RoutingContext.class));

    testRequest(client, HttpMethod.POST, "/test/path")
      .with(
          requestHeader("Content-Encoding", "gzip"),
          requestHeader("Content-Type", "application/json"))
      .expect(
          statusCode(200),
          bodyResponse(expectedJson, "application/text"))
      .sendBuffer(compressedJson, testContext);
  }
}
