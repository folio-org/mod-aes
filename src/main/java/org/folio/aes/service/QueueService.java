package org.folio.aes.service;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public interface QueueService {

  /**
   * Send message to topic using the default queue.
   *
   * @param topic
   * @param msg
   * @return
   */
  CompletableFuture<Boolean> send(String topic, String msg);

  /**
   * Send message to topic for given queue.
   *
   * @param topic
   * @param msg
   * @param queueUrl
   * @return
   */
  CompletableFuture<Boolean> send(String topic, String msg, String queueUrl);

  /**
   * Stop the queue service and clean up resources.
   *
   * @return
   */
  CompletableFuture<Void> stop();

  // /**
  // * Send message to topic in default queue.
  // *
  // * @param topic
  // * @param msg
  // */
  // void send(String topic, String msg, Future<Void> future);
  //
  // /**
  // * Send message to topic of given queue.
  // *
  // * @param topic
  // * @param msg
  // */
  // void send(String queueUrl, String topic, String msg, Future<Void> future);
  //
  // /**
  // * Stop the queue service.
  // *
  // * @param resHandler
  // */
  // public void stop(Handler<AsyncResult<Void>> resHandler);
}
