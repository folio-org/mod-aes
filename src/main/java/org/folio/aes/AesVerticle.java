package org.folio.aes;

import static org.folio.aes.util.Constant.*;

import org.folio.aes.service.AesService;
import org.folio.aes.service.ConfigService;
import org.folio.aes.service.KafkaQueueService;
import org.folio.aes.service.LogQueueService;
import org.folio.aes.service.QueueService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class AesVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(AesVerticle.class);

  @Override
  public void start(Future<Void> fut) throws Exception {

    int port = Integer.parseInt(
      System.getProperty("port",
        System.getProperty("http.port",
          "" + context.config().getInteger("port", 8081))));

    String kafkaUrl = System.getProperty("kafka.url", null);
    QueueService queueService = kafkaUrl == null ? new LogQueueService() : new KafkaQueueService(vertx, kafkaUrl);

    AesService aesService = new AesService(new ConfigService(vertx), queueService);

    Router router = Router.router(vertx);
    // root
    router.route("/").handler(rc -> rc.response().end(MOD_NAME + " is up running"));
    // health checking
    router.route("/admin/health").handler(rc -> rc.response().end("OK"));
    // body mapping
    router.route("/*").handler(BodyHandler.create());
    // filter mapping
    router.route("/*").handler(aesService::prePostHandler);

    vertx.createHttpServer().requestHandler(router::accept).listen(port, rs -> {
      if (rs.succeeded()) {
        fut.complete();
      } else {
        fut.fail(rs.cause());
      }
    });

    logger.info("HTTP server started on port " + port);
  }

}
