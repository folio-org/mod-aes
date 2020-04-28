package org.folio.aes.service;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

public class KafkaService {

  private static Logger logger = LogManager.getLogger();

  public KafkaProducer<String, String> createProducer(Vertx vertx, Properties config) {
    logger.debug("Creating a Kafka producer");
    return KafkaProducer.create(vertx, config, String.class, String.class);
  }

  public KafkaProducerRecord<String, String> createProducerRecord(String topic, String message) {
    logger.debug("Creating a Kafka producer record");
    return KafkaProducerRecord.create(topic, message);
  }
}
