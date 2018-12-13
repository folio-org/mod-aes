package org.folio.aes.service;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Just log the message.
 *
 */
public class LogQueueService implements QueueService {

  private static Logger logger = LoggerFactory.getLogger(LogQueueService.class);

  // @Override
  // public void send(String topic, String msg, Future<Void> future) {
  // logger.debug("topic: " + topic);
  // logger.debug("msg: " + msg);
  // future.complete();
  // }
  //
  // @Override
  // public void send(String queueUrl, String topic, String msg, Future<Void>
  // future) {
  // send(topic, msg, future);
  // }
  //
  // @Override
  // public void stop(Handler<AsyncResult<Void>> resultHandler) {
  // resultHandler.handle(Future.succeededFuture());
  // }

  @Override
  public CompletableFuture<Void> send(String topic, String msg) {
    CompletableFuture<Void> cf = new CompletableFuture<Void>();
    logger.debug("topic: " + topic);
    logger.debug("msg: " + msg);
    cf.complete(null);
    return cf;
  }

  @Override
  public CompletableFuture<Void> send(String topic, String msg, String queueUrl) {
    return send(topic, msg);
  }

  @Override
  public CompletableFuture<Void> stop() {
    CompletableFuture<Void> cf = new CompletableFuture<Void>();
    cf.complete(null);
    return cf;
  }

}
