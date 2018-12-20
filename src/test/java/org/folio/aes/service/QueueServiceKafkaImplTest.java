package org.folio.aes.service;

import static org.mockito.Mockito.*;

import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
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

//  @InjectMocks
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
//    doAnswer(invocation -> Future.succeededFuture(new RecordMetadata())).when(goodKafkaProducer).write(any(), any());
    doAnswer((Answer<AsyncResult<RecordMetadata>>) invocation -> Future.succeededFuture(new RecordMetadata())).when(goodKafkaProducer).write(any(), any());
    qs = new QueueServiceKafkaImpl(vertx, kafkaUrl, kafkaService);
  }

  @Test
  public void testSend() throws Exception {
    for (int i = 0; i < 10; i++) {
      qs.send("topic_a", "msg_" + i);
    }
    Thread.sleep(3000);
    verify(goodKafkaProducer, times(10)).write(any(), any());
  }

  @Test
  public void testStop() throws Exception {
    CompletableFuture<Void> cf = qs.stop();
    cf.get();
    assertTrue(cf.isDone());
  }

}
