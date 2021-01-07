package org.folio.aes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class QueueServiceKafkaImplTest {
  private static String kafkaUrl = "localhost";

  private QueueService qs;

  @Test
  void testSend(Vertx vertx, VertxTestContext testContext, @Mock KafkaService kafkaService,
      @Mock KafkaProducer<String, String> kafkaProducer,
      @Mock KafkaProducerRecord<String, String> producerRecord) throws Exception {
    defaultMocking(kafkaService, kafkaProducer, producerRecord);
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
    @SuppressWarnings("rawtypes")
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      futures.add(qs.send("topic_a", "msg_" + i));
    }
    CompositeFuture.all(futures).onComplete(testContext.succeeding(v -> {
      futures.forEach(future -> assertFutureOk(future));
      verify(kafkaProducer, times(10)).write(any(), any());
      testContext.completeNow();
    }));
  }

  @Test
  void testSendEx(Vertx vertx, VertxTestContext testContext, @Mock KafkaService kafkaService,
      @Mock KafkaProducer<String, String> kafkaProducer,
      @Mock KafkaProducerRecord<String, String> producerRecord) throws Exception {
    when(kafkaService.createProducer(Mockito.any(), any())).thenReturn(kafkaProducer);
    when(kafkaService.createProducerRecord(any(), any())).thenReturn(producerRecord);
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
    doAnswer(invocation -> {
      Handler<AsyncResult<RecordMetadata>> handler = invocation.getArgument(1);
      handler.handle(Future.failedFuture("not OK"));
      return null;
    }).when(kafkaProducer).write(any(), any());
    @SuppressWarnings("rawtypes")
    List<Future> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      futures.add(qs.send("topic_a", "msg_" + i));
    }
    CompositeFuture.join(futures).onComplete(testContext.failing(v -> {
      futures.forEach(future -> assertFutureEx(future));
      verify(kafkaProducer, times(10)).write(any(), any());
      testContext.completeNow();
    }));
  }

  @Test
  void testSendEx2(Vertx vertx, VertxTestContext testContext,
      @Mock KafkaService kafkaService) throws Exception {
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
    when(kafkaService.createProducer(any(), any())).thenThrow(new RuntimeException("bad"));
    Future<Void> future = qs.send("topic_a", "msg_a");
    future.onComplete(testContext.failing(v -> {
      assertEquals(RuntimeException.class, future.cause().getClass());
      testContext.completeNow();
    }));
  }

  @Test
  void testStopOk(Vertx vertx, VertxTestContext testContext, @Mock KafkaService kafkaService,
      @Mock KafkaProducer<String, String> kafkaProducer,
      @Mock KafkaProducerRecord<String, String> producerRecord) throws Exception {
    defaultMocking(kafkaService, kafkaProducer, producerRecord);
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
    testStop(vertx, testContext, kafkaService, kafkaProducer, true);
  }

  @Test
  void testStopEx(Vertx vertx, VertxTestContext testContext, @Mock KafkaService kafkaService,
      @Mock KafkaProducer<String, String> kafkaProducer,
      @Mock KafkaProducerRecord<String, String> producerRecord) throws Exception {
    defaultMocking(kafkaService, kafkaProducer, producerRecord);
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
    testStop(vertx, testContext, kafkaService, kafkaProducer, false);
  }

  private void testStop(Vertx vertx, VertxTestContext testContext, KafkaService kafkaService,
      KafkaProducer<String, String> kafkaProducer, boolean ok) throws Exception {
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, false);
    doAnswer(invocation -> {
      Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
      if (ok) {
        handler.handle(Future.succeededFuture());
      } else {
        handler.handle(Future.failedFuture("not OK"));
      }
      return null;
    }).when(kafkaProducer).close(any());
    Future<Void> f0 = qs.send("topic_0", "msg_0");
    Future<Void> fa = qs.send("topic_a", "msg_a", "queue_a");
    Future<Void> fb = qs.send("topic_b", "msg_b", "queue_b");
    CompositeFuture.all(f0, fa, fb).onComplete(testContext.succeeding(v -> {
      Future<Void> f = qs.stop();
      f.onComplete(res -> {
        if (ok) {
          assertFutureOk(f);
        } else {
          assertFutureEx(f);
        }
        testContext.completeNow();
      });
    }));
  }

  private static void assertFutureOk(@SuppressWarnings("rawtypes") Future future) {
    assertTrue(future.isComplete());
    assertTrue(!future.failed());
  }

  private static void assertFutureEx(@SuppressWarnings("rawtypes") Future future) {
    assertTrue(future.isComplete());
    assertTrue(future.failed());
  }

  private void defaultMocking(KafkaService kafkaService,
      KafkaProducer<String, String> kafkaProducer,
      KafkaProducerRecord<String, String> producerRecord) {
    when(kafkaService.createProducer(any(), any())).thenReturn(kafkaProducer);
    when(kafkaService.createProducerRecord(any(), any())).thenReturn(producerRecord);
    doAnswer(invocation -> {
      Handler<AsyncResult<RecordMetadata>> handler = invocation.getArgument(1);
      handler.handle(Future.succeededFuture(new RecordMetadata()));
      return null;
    }).when(kafkaProducer).write(any(), any());
  }
}
