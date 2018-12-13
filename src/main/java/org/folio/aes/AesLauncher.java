package org.folio.aes;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

// Just used for dev testing
public class AesLauncher {

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
  }

  public static void main(String[] args) {
    int processors = Runtime.getRuntime().availableProcessors();
//    DeploymentOptions options = new DeploymentOptions().setInstances(processors * 2);
    DeploymentOptions options = new DeploymentOptions().setInstances(1);
    Vertx.vertx(new VertxOptions().setBlockedThreadCheckInterval(60000)).deployVerticle(AesVerticle.class.getName(),
      options);
  }

}
