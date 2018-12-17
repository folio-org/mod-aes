package org.folio.aes.service;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Just log the message.
 *
 */
public class QueueServiceLogImpl implements QueueService {

  private static Logger logger = LoggerFactory.getLogger(QueueServiceLogImpl.class);

  @Override
  public CompletableFuture<Void> send(String topic, String msg) {
    CompletableFuture<Void> cf = new CompletableFuture<>();
    logger.info("topic: " + topic);
    logger.info(msg);
    cf.complete(null);
    return cf;
  }

  @Override
  public CompletableFuture<Void> send(String topic, String msg, String queueUrl) {
    return send(topic, msg);
  }

  @Override
  public CompletableFuture<Void> stop() {
    CompletableFuture<Void> cf = new CompletableFuture<>();
    cf.complete(null);
    return cf;
  }

}
