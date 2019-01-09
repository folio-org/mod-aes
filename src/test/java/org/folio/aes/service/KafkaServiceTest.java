package org.folio.aes.service;

import java.util.Properties;

import static org.junit.Assert.*;

import org.apache.kafka.common.config.ConfigException;
import org.junit.Test;

import io.vertx.core.Vertx;

public class KafkaServiceTest {

  private KafkaService KafkaService = new KafkaService();

  @Test(expected = ConfigException.class)
  public void testCreateProducer() {
    Vertx vertx = Vertx.vertx();
    try {
      assertNotNull(KafkaService.createProducer(vertx, new Properties()));
    } finally {
      vertx.close();
    }
  }

  @Test
  public void testCreateProducerRecord() {
    System.out.println(KafkaService.createProducerRecord("test", "test"));
    assertNotNull(KafkaService.createProducerRecord("test", "test"));
  }

}
