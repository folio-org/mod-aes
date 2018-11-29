package org.folio.aes.service;

import io.vertx.core.Future;

public class LogQueueService implements QueueService {

  @Override
  public void send(String topic, String msg) {
    System.out.println("topic: " + topic);
    System.out.println(msg);
  }

  @Override
  public void send(String topic, String msg, Future<Void> future) {
    send(topic, msg);
  }

  @Override
  public void send(String queueUrl, String topic, String msg) {
    send(topic, msg);
  }

  @Override
  public void send(String queueUrl, String topic, String msg, Future<Void> future) {
    send(topic, msg);
  }

}
