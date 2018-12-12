package org.folio.aes;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

// Just used for dev testing
public class AesLauncher {

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
  }

  public static void main(String[] args) {
    int processors = Runtime.getRuntime().availableProcessors();
    DeploymentOptions options = new DeploymentOptions().setInstances(processors * 2);
    Vertx.vertx().deployVerticle(AesVerticle.class.getName(), options);
  }

}
