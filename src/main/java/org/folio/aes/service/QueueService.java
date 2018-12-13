package org.folio.aes.service;

import java.util.concurrent.CompletableFuture;

public interface QueueService {

  /**
   * Send message to topic using the default queue.
   *
   * @param topic
   * @param msg
   * @return
   */
  CompletableFuture<Void> send(String topic, String msg);

  /**
   * Send message to topic of given queue.
   *
   * @param topic
   * @param msg
   * @param queueUrl
   * @return
   */
  CompletableFuture<Void> send(String topic, String msg, String queueUrl);

  /**
   * Stop the queue service and clean up resources.
   *
   * @return
   */
  CompletableFuture<Void> stop();

}
