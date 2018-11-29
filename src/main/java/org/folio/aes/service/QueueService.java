package org.folio.aes.service;

import io.vertx.core.Future;

public interface QueueService {

  void send(String topic, String msg);

  void send(String topic, String msg, Future<Void> future);

  void send(String queueUrl, String topic, String msg);

  void send(String queueUrl, String topic, String msg, Future<Void> future);

}
