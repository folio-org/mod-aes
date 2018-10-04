package org.folio.aes.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.kafka.clients.producer.ProducerConfig;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

/**
 * Test environment in dev EC2
 *
 * <pre>
 * cd ~/hji/test/kafka-docker/
 * docker-compose -f dc.yml up -d
 * sleep 5
 * docker run -d --rm --name kafdrop -e ZK_HOSTS=10.23.33.20:2181 -e LISTEN=9010 -p9010:9010 thomsch98/kafdrop
 *
 * http://10.23.33.20:9010
 *
 * docker stop kafdrop
 * docker-compose -f dc.yml down
 * </pre>
 *
 * @author hji
 *
 */
public class KafkaService implements QueueService {

  private static Logger logger = LoggerFactory.getLogger(KafkaService.class);

  private Vertx vertx;
  private ConcurrentMap<String, KafkaProducer<String, String>> producers = new ConcurrentHashMap<>();

  public KafkaService(Vertx vertx) {
    this.vertx = vertx;
  }

  public void send(String kafkaUrl, String topic, String msg) {
    send(kafkaUrl, topic, msg, Future.future());
  }

  @Override
  public void send(String kafkaUrl, String topic, String msg, Future<Void> future) {
    getProducer(kafkaUrl, ar -> {
      if (ar.succeeded()) {
        ar.result().write(KafkaProducerRecord.create(topic, msg), res -> {
          if (res.succeeded()) {
            logger.debug("Send OK: " + msg);
            future.complete();
          } else {
            logger.warn("Send not OK: " + msg);
            future.fail(res.cause());
          }
        });
      } else {
        future.fail(ar.cause());
      }
    });
  }

  @SuppressWarnings("rawtypes")
  public void stop(Handler<AsyncResult<Void>> resultHandler) {
    logger.info("Stop Kafka producer(s)");
    List<Future> futures = new ArrayList<>();
    producers.forEach((k, v) -> {
      Future future = Future.future();
      futures.add(future);
      v.close(ar -> {
        if (ar.succeeded()) {
          logger.info(k + " is stopped");
        } else {
          logger.warn(k + " failed to stop");
        }
        future.complete();
      });
    });
    CompositeFuture.join(futures).setHandler(ar -> resultHandler.handle(Future.succeededFuture()));
  }

  private KafkaProducer<String, String> createProducer(String kafkaUrl) {
    logger.info("Create Kafka producer for " + kafkaUrl);
    Properties config = new Properties();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
    KafkaProducer<String, String> producer = KafkaProducer.create(vertx, config, String.class, String.class);
    producer.exceptionHandler(e -> logger.warn(e));
    return producer;
  }

  private void getProducer(String kafkaUrl, Handler<AsyncResult<KafkaProducer<String, String>>> resultHandler) {
    logger.debug("Get Kafka producer for " + kafkaUrl);
    KafkaProducer<String, String> producer = producers.get(kafkaUrl);
    if (producer != null) {
      logger.debug("Return an existing producer " + System.identityHashCode(producer));
      resultHandler.handle(Future.succeededFuture(producer));
    } else {
      KafkaProducer<String, String> newProducer = createProducer(kafkaUrl);
      KafkaProducer<String, String> p = producers.putIfAbsent(kafkaUrl, newProducer);
      // just in case that a duplicate is created due to competition
      if (p != null) {
        logger.warn("Close a producer duplicate");
        newProducer.close();
        resultHandler.handle(Future.succeededFuture(p));
      } else {
        resultHandler.handle(Future.succeededFuture(newProducer));
      }
    }
  }

  // just for quick testing
  public static void main(String[] args) {
    String kafkaUrl = "10.23.33.20:9092"; // dev-dmz instance
    // String kafkaUrl = "10.23.32.4:9092"; // another instance from Tanuja

    Vertx vertx = Vertx.vertx();
    KafkaService kafkaService = new KafkaService(vertx);

    @SuppressWarnings("rawtypes")
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Future<Void> future = Future.future();
      futures.add(future);
      kafkaService.send(kafkaUrl, "hji-test", "testing " + i, future);
    }
    CompositeFuture.join(futures).setHandler(ar -> {
      kafkaService.stop(res -> vertx.close());
    });
  }

}
