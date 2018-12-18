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
    assertNotNull(KafkaService.createProducer(Vertx.vertx(), new Properties()));
  }

}
