package org.folio.aes.service;

import static org.mockito.Mockito.*;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;

public class QueueServiceKafkaImplTest {

  @Mock
  private KafkaService kafkaService;

  @Mock
  private KafkaProducer<String, String> kafkaProducer;

  @Mock
  private KafkaProducerRecord<String, String> producerRecord;

  private static String kafkaUrl = "localhost";
  private static Vertx vertx;
  private QueueService qs;

  @BeforeClass
  public static void setUpOnce() {
    vertx = Vertx.vertx();
  }

  @AfterClass
  public static void tearDownOnce() {
    vertx.close();
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(kafkaService.createProducer(any(), any())).thenReturn(kafkaProducer);
    when(kafkaService.createProducerRecord(any(), any())).thenReturn(producerRecord);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Handler<AsyncResult<RecordMetadata>> handler = invocation.getArgument(1);
        handler.handle(Future.succeededFuture(new RecordMetadata()));
        return null;
      }
    }).when(kafkaProducer).write(any(), any());
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService, true);
  }

  @Test
  public void testSend() throws Exception {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      futures.add(qs.send("topic_a", "msg_" + i));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
    futures.forEach(future -> assertFutureOk(future));
    verify(kafkaProducer, times(10)).write(any(), any());
  }

  @Test
  public void testSendEx() throws Exception {
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

  @Test(expected = ExecutionException.class)
  public void testSendEx2() throws Exception {
    when(kafkaService.createProducer(any(), any())).thenThrow(new RuntimeException("bad"));
    CompletableFuture<Void> cf = qs.send("topic_a", "msg_a");
    cf.get();
  }

  @Test
  public void testStopOk() throws Exception {
    testStop(true);
  }

  @Test(expected = ExecutionException.class)
  public void testStopEx() throws Exception {
    testStop(false);
  }

  private void testStop(boolean ok) throws Exception {
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

}
