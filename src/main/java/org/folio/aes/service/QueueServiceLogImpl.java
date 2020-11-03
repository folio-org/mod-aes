package org.folio.aes.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Future;

/**
 * Just log the message.
 *
 */
public class QueueServiceLogImpl implements QueueService {

  private static Logger logger = LogManager.getLogger();

  @Override
  public Future<Void> send(String topic, String msg) {
    logger.info("topic: {}", topic);
    logger.info(msg);
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> send(String topic, String msg, String queueUrl) {
    return send(topic, msg);
  }

  @Override
  public Future<Void> stop() {
    return Future.succeededFuture();
  }
}
