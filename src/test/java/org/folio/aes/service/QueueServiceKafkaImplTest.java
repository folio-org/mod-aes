package org.folio.aes.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class QueueServiceKafkaImplTest {
  private static String kafkaUrl = "localhost";

  private QueueService qs;

  @Test
  public void testSend(Vertx vertx, @Mock KafkaService kafkaService,
      @Mock KafkaProducer<String, String> kafkaProducer,
      @Mock KafkaProducerRecord<String, String> producerRecord) throws Exception {
    defaultMocking(kafkaService, kafkaProducer, producerRecord);
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      futures.add(qs.send("topic_a", "msg_" + i));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
    futures.forEach(future -> assertFutureOk(future));
    verify(kafkaProducer, times(10)).write(any(), any());
  }

  @Test
  public void testSendEx(Vertx vertx, @Mock KafkaService kafkaService,
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
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      futures.add(qs.send("topic_a", "msg_" + i));
    }
    futures.forEach(future -> {
      try {
        future.get();
      } catch (Exception e) {
        // just want to finish
      }
    });
    futures.forEach(future -> assertFutureEx(future));
    verify(kafkaProducer, times(10)).write(any(), any());
  }

  @Test
  public void testSendEx2(Vertx vertx, @Mock KafkaService kafkaService) throws Exception {
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
    when(kafkaService.createProducer(any(), any())).thenThrow(new RuntimeException("bad"));
    assertThrows(
        ExecutionException.class,
        () -> {
          CompletableFuture<Void> cf = qs.send("topic_a", "msg_a");
          cf.get();
        });
  }

  @Test
  public void testStopOk(Vertx vertx, @Mock KafkaService kafkaService,
      @Mock KafkaProducer<String, String> kafkaProducer,
      @Mock KafkaProducerRecord<String, String> producerRecord) throws Exception {
    defaultMocking(kafkaService, kafkaProducer, producerRecord);
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
    testStop(vertx, kafkaService, kafkaProducer, true);
  }

  @Test
  public void testStopEx(Vertx vertx, @Mock KafkaService kafkaService,
      @Mock KafkaProducer<String, String> kafkaProducer,
      @Mock KafkaProducerRecord<String, String> producerRecord) throws Exception {
    defaultMocking(kafkaService, kafkaProducer, producerRecord);
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
    assertThrows(
        ExecutionException.class,
        () -> testStop(vertx, kafkaService, kafkaProducer, false));
  }

  private void testStop(Vertx vertx, KafkaService kafkaService,
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
    CompletableFuture<Void> cf0 = qs.send("topic_0", "msg_0");
    CompletableFuture<Void> cfa = qs.send("topic_a", "msg_a", "queue_a");
    CompletableFuture<Void> cfb = qs.send("topic_b", "msg_b", "queue_b");
    cf0.get();
    cfa.get();
    cfb.get();
    CompletableFuture<Void> cf = qs.stop();
    cf.get();
    assertFutureOk(cf);
  }

  private static void assertFutureOk(CompletableFuture<Void> future) {
    assertTrue(future.isDone());
    assertTrue(!future.isCancelled());
    assertTrue(!future.isCompletedExceptionally());
  }

  private static void assertFutureEx(CompletableFuture<Void> future) {
    assertTrue(future.isDone());
    assertTrue(!future.isCancelled());
    assertTrue(future.isCompletedExceptionally());
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
