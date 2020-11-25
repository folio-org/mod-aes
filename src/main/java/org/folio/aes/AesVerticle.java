package org.folio.aes;

import static org.folio.aes.util.AesConstants.MOD_NAME;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.aes.service.AesService;
import org.folio.aes.service.KafkaService;
import org.folio.aes.service.QueueServiceKafkaImpl;
import org.folio.aes.service.QueueServiceLogImpl;
import org.folio.aes.service.RuleServiceConfigImpl;

public class AesVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger();

  private AesService aesService = new AesService();

  @Override
  public void start(Promise<Void> promise) throws Exception {

    final int port = Integer.getInteger("port",
        Integer.getInteger("http.port",
            context.config().getInteger("port", 8081)));

    String kafkaUrl = System.getProperty("kafka.url", null);

    aesService.setRuleService(new RuleServiceConfigImpl(vertx));
    aesService.setQueueService(
        kafkaUrl == null
          ? new QueueServiceLogImpl()
          : new QueueServiceKafkaImpl(vertx, kafkaUrl, new KafkaService(), false));

    Router router = Router.router(vertx);
    // root
    router.route("/").handler(rc -> rc.response().end(MOD_NAME + " is up running"));
    // health checking
    router.route("/admin/health").handler(rc -> rc.response().end("OK"));
    // body mapping
    router.route("/*").handler(BodyHandler.create());
    // filter mapping
    router.route("/*").handler(aesService::prePostHandler);

    HttpServerOptions options = new HttpServerOptions()
        .setCompressionSupported(true)
        .setDecompressionSupported(true)
        .setPort(port);
    vertx.createHttpServer(options).requestHandler(router).listen(rs -> {
      if (rs.succeeded()) {
        logger.info("HTTP server started on port {}", port);
        promise.complete();
      } else {
        logger.error("HTTP server failed to start on port {}", port, rs.cause());
        promise.fail(rs.cause());
      }
    });
  }

  @Override
  public void stop(Promise<Void> promise) {
    if (aesService != null) {
      aesService.stop()
          .onComplete(promise)
          .onFailure(logger::warn);
    } else {
      promise.complete();
    }
  }
}
