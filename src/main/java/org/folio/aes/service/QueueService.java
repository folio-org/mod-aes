package org.folio.aes.service;

import io.vertx.core.Future;

public interface QueueService {

  /**
   * Send message to topic using the default queue.
   *
   * @param topic the topic to send the message
   * @param msg the message to be sent
   * @return a future to determine whether or not the send succeeded
   */
  Future<Void> send(String topic, String msg);

  /**
   * Send message to topic of given queue.
   *
   * @param topic the topic to send the message
   * @param msg the message to be sent
   * @param queueUrl the queue's URL
   * @return a future to determine whether or not the send succeeded
   */
  Future<Void> send(String topic, String msg, String queueUrl);

  /**
   * Stop the queue service and clean up resources.
   *
   * @return a future to determine whether or not the service stopped successfully
   */
  Future<Void> stop();

}
