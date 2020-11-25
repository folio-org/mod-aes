package org.folio.aes.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class QueueServiceLogImplTest {

  private QueueService qs = new QueueServiceLogImpl();

  @Test
  void testSendOne(VertxTestContext testContext) throws Exception {
    Future<Void> f = qs.send(null, null);
    f.onComplete(testContext.succeeding(v -> {
      assertTrue(f.isComplete());
      testContext.completeNow();
    }));
  }

  @Test
  void testSendTwo(VertxTestContext testContext) throws Exception {
    Future<Void> f = qs.send(null, null, null);
    f.onComplete(testContext.succeeding(v -> {
      assertTrue(f.isComplete());
      testContext.completeNow();
    }));
  }

  @Test
  void testStop(VertxTestContext testContext) throws Exception {
    Future<Void> f = qs.stop();
    f.onComplete(testContext.succeeding(v -> {
      assertTrue(f.isComplete());
      testContext.completeNow();
    }));
  }

}
