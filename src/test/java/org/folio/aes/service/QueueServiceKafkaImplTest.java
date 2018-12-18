package org.folio.aes.service;

import static org.mockito.Mockito.*;

import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

public class QueueServiceKafkaImplTest {

  // private QueueService qs = new QueueServiceKafkaImpl(vertx, kafkaUrl);

  @Test
  public void testSendOne() throws Exception {

    List mockedList = mock(List.class);

    // using mock object - it does not throw any "unexpected interaction" exception
    mockedList.add("one");
    mockedList.clear();

    // selective, explicit, highly readable verification
    verify(mockedList).add("one");
    verify(mockedList).clear();

    LinkedList mockedList2 = mock(LinkedList.class);

    // stubbing appears before the actual execution
    when(mockedList2.get(0)).thenReturn("first");

    // the following prints "first"
    System.out.println(mockedList2.get(0));

    // the following prints "null" because get(999) was not stubbed
    System.out.println(mockedList2.get(999));

  }

}
