package org.folio.aes.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import java.util.Properties;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class KafkaServiceTest {

  private KafkaService kafkaService = new KafkaService();

  @Test
  void testCreateProducer(Vertx vertx) {
    final Properties properties = new Properties();
    final KafkaException expectedException =
        assertThrows(KafkaException.class, () -> kafkaService.createProducer(vertx, properties));

    assertThat(expectedException.getCause(), isA(ConfigException.class));
  }

  @Test
  void testCreateProducerRecord() {
    assertNotNull(kafkaService.createProducerRecord("test", "test"));
  }
}
