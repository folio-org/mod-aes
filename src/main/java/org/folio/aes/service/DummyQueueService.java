package org.folio.aes.service;

import io.vertx.core.Future;

public class DummyQueueService implements QueueService {

  @Override
  public void send(String queueUrl, String topic, String msg) {
    send(queueUrl, topic, msg, null);
  }

  @Override
  public void send(String queueUrl, String topic, String msg, Future<Void> future) {
    System.out.println("topic: " + topic);
    System.out.println(msg);
  }

}
