package org.folio.aes.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

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
public class KafkaQueueService implements QueueService {

  private static Logger logger = LoggerFactory.getLogger(KafkaQueueService.class);

  private Vertx vertx;
  private String defaultKafkaUrl;

  // keep Kafka producers
  private ConcurrentMap<String, KafkaProducer<String, String>> producers = new ConcurrentHashMap<>();

  public KafkaQueueService(Vertx vertx, String kafkaUrl) {
    this.vertx = vertx;
    this.defaultKafkaUrl = kafkaUrl;
    producers.put(kafkaUrl, createProducer(kafkaUrl));
  }

  @Override
  public CompletableFuture<Void> send(String topic, String msg) {
    return send(defaultKafkaUrl, topic, msg);
  }

  @Override
  public CompletableFuture<Void> send(String kafkaUrl, String topic, String msg) {
    CompletableFuture<Void> cf = new CompletableFuture<Void>();

    getOrCreateProducer(kafkaUrl).thenCompose(p -> CompletableFuture.runAsync(() -> {
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
            logger.info(k + " is stopped.");
          } else {
            logger.warn(k + " was failed to stop.");
          }
        });
      }));
    });
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
  }

//  private void getProducer(String kafkaUrl, Handler<AsyncResult<KafkaProducer<String, String>>> resultHandler) {
//    logger.debug("Get Kafka producer for " + kafkaUrl);
//    KafkaProducer<String, String> producer = producers.get(kafkaUrl);
//    if (producer != null) {
//      logger.debug("Return an existing producer " + System.identityHashCode(producer));
//      resultHandler.handle(Future.succeededFuture(producer));
//    } else {
//      KafkaProducer<String, String> newProducer = createProducer(kafkaUrl);
//      KafkaProducer<String, String> p = producers.putIfAbsent(kafkaUrl, newProducer);
//      // just in case that a duplicate is created due to competition
//      if (p != null) {
//        logger.warn("Close a producer duplicate");
//        newProducer.close();
//        resultHandler.handle(Future.succeededFuture(p));
//      } else {
//        resultHandler.handle(Future.succeededFuture(newProducer));
//      }
//    }
//  }

  private CompletableFuture<KafkaProducer<String, String>> getOrCreateProducer(String kafkaUrl) {
    logger.debug("Get Kafka producer for " + kafkaUrl);
    CompletableFuture<KafkaProducer<String, String>> cf = new CompletableFuture<>();
    KafkaProducer<String, String> producer = producers.get(kafkaUrl);
    if (producer != null) {
      logger.debug("Return an existing producer " + System.identityHashCode(producer));
      cf.complete(producer);
    } else {
      CompletableFuture.supplyAsync(() -> createProducer(kafkaUrl)).thenAccept(newProducer -> {
        KafkaProducer<String, String> p = producers.putIfAbsent(kafkaUrl, newProducer);
        // just in case that a duplicate is created due to competition
        if (p != null) {
          logger.warn("Close a producer duplicate " + System.identityHashCode(newProducer));
          newProducer.close();
          cf.complete(p);
        } else {
          cf.complete(newProducer);
        }
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

  // quick test
  public static void main(String[] args) throws Exception {
    String kafkaUrl = "";
    // String kafkaUrl = "10.23.33.20:9092"; // dev instance
    Vertx vertx = Vertx.vertx();
    KafkaQueueService kafkaService = new KafkaQueueService(vertx, kafkaUrl);
    @SuppressWarnings("rawtypes")
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      futures.add(kafkaService.send("hji-test", "testing " + i));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
  }

}
