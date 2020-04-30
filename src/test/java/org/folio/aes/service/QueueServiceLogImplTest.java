package org.folio.aes.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

public class QueueServiceLogImplTest {

  private QueueService qs = new QueueServiceLogImpl();

  @Test
  public void testSendOne() throws Exception {
    CompletableFuture<Void> cf = qs.send(null, null);
    cf.get();
    assertTrue(cf.isDone());
  }

  @Test
  public void testSendTwo() throws Exception {
    CompletableFuture<Void> cf = qs.send(null, null, null);
    cf.get();
    assertTrue(cf.isDone());
  }

  @Test
  public void testStop() throws Exception {
    CompletableFuture<Void> cf = qs.stop();
    cf.get();
    assertTrue(cf.isDone());
  }

}
