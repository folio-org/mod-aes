package org.folio.aes.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.kafka.clients.producer.ProducerConfig;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

/**
 * Send message to Kafka queue.
 *
 * Notes: Test environment in dev EC2
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
public class QueueServiceKafkaImpl implements QueueService {

  private static Logger logger = LoggerFactory.getLogger(QueueServiceKafkaImpl.class);

  private Vertx vertx;
  private String defaultKafkaUrl;

  // keep Kafka producers
  private ConcurrentMap<String, KafkaProducer<String, String>> producers = new ConcurrentHashMap<>();

  public QueueServiceKafkaImpl(Vertx vertx, String kafkaUrl) {
    this.vertx = vertx;
    this.defaultKafkaUrl = kafkaUrl;
    producers.put(kafkaUrl, createProducer(kafkaUrl));
  }

  @Override
  public CompletableFuture<Void> send(String topic, String msg) {
    return send(topic, msg, defaultKafkaUrl);
  }

  @Override
  public CompletableFuture<Void> send(String topic, String msg, String kafkaUrl) {
    CompletableFuture<Void> cf = new CompletableFuture<Void>();
    getOrCreateProducer(kafkaUrl)
      .thenCompose(p -> CompletableFuture.runAsync(() -> {
        p.write(KafkaProducerRecord.create(topic, msg), res -> {
          if (res.succeeded()) {
            logger.debug("Send OK: " + msg);
            cf.complete(null);
          } else {
            logger.warn("Send not OK: " + msg);
            cf.completeExceptionally(res.cause());
          }
        });
      })).handle((res, ex) -> {
        if (ex != null) {
          logger.warn(ex);
          cf.completeExceptionally(ex);
          return ex;
        }
        return res;
      });
    return cf;
  }

  @Override
  public CompletableFuture<Void> stop() {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    producers.forEach((k, v) -> {
      futures.add(CompletableFuture.runAsync(() -> {
        v.close(ar -> {
          if (ar.succeeded()) {
            logger.info(k + " stopped.");
          } else {
            logger.warn(k + " failed to stop.");
          }
        });
      }));
    });
    return CompletableFuture.allOf(
      futures.toArray(new CompletableFuture[futures.size()]));
  }

  private CompletableFuture<KafkaProducer<String, String>> getOrCreateProducer(String kafkaUrl) {
    logger.debug("Get Kafka producer for " + kafkaUrl);
    CompletableFuture<KafkaProducer<String, String>> cf = new CompletableFuture<>();
    KafkaProducer<String, String> producer = producers.get(kafkaUrl);
    if (producer != null) {
      logger.debug("Return an existing producer " + System.identityHashCode(producer));
      cf.complete(producer);
    } else {
      CompletableFuture.supplyAsync(() -> {
        return createProducer(kafkaUrl);
      }).thenAccept(newProducer -> {
        KafkaProducer<String, String> p = producers.putIfAbsent(kafkaUrl, newProducer);
        // just in case that a duplicate is created due to competition
        if (p != null) {
          logger.warn("Close a producer duplicate " + System.identityHashCode(newProducer));
          CompletableFuture.runAsync(() -> newProducer.close());
          cf.complete(p);
        } else {
          cf.complete(newProducer);
        }
      }).handle((res, ex) -> {
        if (ex != null) {
          logger.warn(ex);
          cf.completeExceptionally(ex);
          return ex;
        }
        return res;
      });
    }
    return cf;
  }

  private KafkaProducer<String, String> createProducer(String kafkaUrl) {
    logger.info("Create Kafka producer for " + kafkaUrl);
    Properties config = new Properties();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
    KafkaProducer<String, String> producer = KafkaProducer.create(vertx, config, String.class, String.class);
    producer.exceptionHandler(e -> logger.warn(e));
    return producer;
  }

  // for development
  public static void main(String[] args) throws Exception {
    String kafkaUrl = "10.23.33.20:9092";
    Vertx vertx = Vertx.vertx();
    QueueService queueService = new QueueServiceKafkaImpl(vertx, kafkaUrl);
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      futures.add(queueService.send("hji-test", "testing " + i));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
    queueService.stop().thenRun(() -> vertx.close());
  }

}
