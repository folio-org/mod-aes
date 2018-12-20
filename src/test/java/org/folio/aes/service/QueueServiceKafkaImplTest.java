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
  private KafkaProducer<String, String> goodKafkaProducer;

  @Mock
  private KafkaProducer<String, String> badKafkaProducer;

  @Mock
  private KafkaProducerRecord<String, String> kafkaProducerRecord;

  // @InjectMocks
  private QueueService qs;

  private static Vertx vertx;
  private static String kafkaUrl;

  @BeforeClass
  public static void setupBeforeClass() {
    vertx = Vertx.vertx();
    kafkaUrl = "localhost";
  }

  @AfterClass
  public static void tearDown() {
    vertx.close();
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(kafkaService.createProducer(any(), any())).thenReturn(goodKafkaProducer);
    when(kafkaService.createProducerRecord(any(), any())).thenReturn(kafkaProducerRecord);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Handler<AsyncResult<RecordMetadata>> handler = invocation.getArgument(1);
        handler.handle(Future.succeededFuture(new RecordMetadata()));
        return null;
      }
    }).when(goodKafkaProducer).write(any(), any());
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService);

  }

  @Test
  public void testSend() throws Exception {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      futures.add(qs.send("topic_a", "msg_" + i));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
    futures.forEach(future -> assertFutureOk(future));
    verify(goodKafkaProducer, times(10)).write(any(), any());
  }

  @Test
  public void testSendEx() throws Exception {
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Handler<AsyncResult<RecordMetadata>> handler = invocation.getArgument(1);
        handler.handle(Future.failedFuture("not OK"));
        return null;
      }
    }).when(goodKafkaProducer).write(any(), any());
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
    verify(goodKafkaProducer, times(10)).write(any(), any());
  }

  private void testStop(boolean ok) throws Exception {
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
        if (ok) {
          handler.handle(Future.succeededFuture());
        } else {
          handler.handle(Future.failedFuture("not OK"));
        }
        return null;
      }
    }).when(goodKafkaProducer).close(any());
    qs.send("topic_a", "msg_a", "queue_a");
    qs.send("topic_b", "msg_b", "queue_b");
    CompletableFuture<Void> cf = qs.stop();
    cf.get();
    assertFutureOk(cf);
  }

  @Test
  public void testStopOk() throws Exception {
    testStop(true);
  }

  @Test(expected = ExecutionException.class)
  public void testStopEx() throws Exception {
    testStop(false);
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
