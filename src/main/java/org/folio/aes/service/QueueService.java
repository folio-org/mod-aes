package org.folio.aes.service;

import io.vertx.core.Future;

public interface QueueService {

  /**
   * Send message to topic using the default queue.
   *
   * @param topic
   * @param msg
   * @return
   */
  Future<Void> send(String topic, String msg);

  /**
   * Send message to topic of given queue.
   *
   * @param topic
   * @param msg
   * @param queueUrl
   * @return
   */
  Future<Void> send(String topic, String msg, String queueUrl);

  /**
   * Stop the queue service and clean up resources.
   *
   * @return
   */
  Future<Void> stop();

}
