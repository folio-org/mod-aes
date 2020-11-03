package org.folio.aes.service;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;

/**
 * Send message to Kafka queue.
 *
 * <pre>
 * # set up test environment
 * cd ~/hji/test/kafka-docker/
 * docker-compose -f dc.yml up -d
 * sleep 5
 * docker run -d --rm --name kafdrop -e ZK_HOSTS=10.23.33.20:2181 -e LISTEN=9010 -p9010:9010 thomsch98/kafdrop
 * # check by UI
 * http://10.23.33.20:9010
 * # clean up environment
 * docker stop kafdrop
 * docker-compose -f dc.yml down
 * # test code
 * public static void main(String[] args) throws Exception {
 *   String kafkaUrl = "10.23.33.20:9092";
 *   Vertx vertx = Vertx.vertx();
 *   QueueService queueService = new QueueServiceKafkaImpl(vertx, kafkaUrl, new KafkaService(), false);
 *   List<CompletableFuture<Void>> futures = new ArrayList<>();
 *   for (int i = 0; i < 10; i++) {
 *     futures.add(queueService.send("hji-test", "testing " + i));
 *   }
 *   CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
 *   queueService.stop().thenRun(vertx::close);
 * }
 * </pre>
 */
public class QueueServiceKafkaImpl implements QueueService {

  private static Logger logger = LogManager.getLogger();

  private Vertx vertx;
  private String defaultKafkaUrl;
  private KafkaService kafkaService;
  private ConcurrentMap<String, KafkaProducer<String, String>> producers = new ConcurrentHashMap<>();

  public QueueServiceKafkaImpl(Vertx vertx, String kafkaUrl, KafkaService kafkaService, boolean lazy) {
    this.vertx = vertx;
    this.defaultKafkaUrl = kafkaUrl;
    this.kafkaService = kafkaService;
    if (!lazy) {
      producers.put(kafkaUrl, createProducer(kafkaUrl));
    }
  }

  @Override
  public Future<Void> send(String topic, String msg) {
    return send(topic, msg, defaultKafkaUrl);
  }

  @Override
  public Future<Void> send(String topic, String msg, String kafkaUrl) {
    return getOrCreateProducer(kafkaUrl)
      .compose(p -> writeToProducer(p, topic, msg));
  }

  @Override
  public Future<Void> stop() {
    @SuppressWarnings("rawtypes")
    final List<Future> futures = new ArrayList<>();

    producers.forEach((k, v) -> futures.add(Future.future(promise -> v.close(ar -> {
        if (ar.succeeded()) {
          logger.info("{} stopped.", k);
          promise.complete();
        } else {
          logger.warn("{} failed to stop.", k, ar.cause());
          promise.fail(ar.cause());
        }
      }))));

    return CompositeFuture.all(futures).mapEmpty();
  }

  private Future<Void> writeToProducer(KafkaProducer<String, String> producer, String topic,
      String message) {
    final Promise<Void> promise = Promise.promise();

    producer.write(kafkaService.createProducerRecord(topic, message), res -> {
      if (res.succeeded()) {
        logger.info("Send OK: {}", message);
        promise.complete();
      } else {
        logger.warn("Send not OK: {}", message);
        promise.fail(res.cause());
      }
    });

    return promise.future();
  }

  private Future<KafkaProducer<String, String>> getOrCreateProducer(String kafkaUrl) {
    logger.debug("Get Kafka producer for {}", kafkaUrl);

    final Promise<KafkaProducer<String, String>> promise = Promise.promise();
    final KafkaProducer<String, String> producer = producers.get(kafkaUrl);

    if (producer != null) {
      logger.debug("Return an existing producer {}", System.identityHashCode(producer));
      promise.complete(producer);
    } else {
      vertx.runOnContext(v -> {
        try {
          var newProducer = createProducer(kafkaUrl);
          KafkaProducer<String, String> p = producers.putIfAbsent(kafkaUrl, newProducer);
          // just in case that a duplicate is created due to competition
          if (p != null) {
            logger.warn("Close a producer duplicate {}", System.identityHashCode(newProducer));
            newProducer.close();
            promise.complete(p);
          } else {
            promise.complete(newProducer);
          }
        } catch (Exception ex) {
          if (ex != null) {
            logger.warn(ex);
            promise.fail(ex);
          }
        }
      });
    }

    return promise.future();
  }

  private KafkaProducer<String, String> createProducer(String kafkaUrl) {
    logger.info("Create Kafka producer for {}", kafkaUrl);
    Properties config = new Properties();
    config.put(BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
    KafkaProducer<String, String> producer = kafkaService.createProducer(vertx, config);
    producer.exceptionHandler(e -> logger.warn(e));
    logger.debug("returning producer");
    return producer;
  }
}
