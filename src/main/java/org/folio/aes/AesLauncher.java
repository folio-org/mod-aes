package org.folio.aes;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

// Just for development
public class AesLauncher {

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  public static void main(String[] args) {
    int processors = Runtime.getRuntime().availableProcessors();
    DeploymentOptions dopt = new DeploymentOptions().setInstances(processors * 2);
    VertxOptions vopt = new VertxOptions().setBlockedThreadCheckInterval(60000);
    Vertx.vertx(vopt).deployVerticle(AesVerticle.class.getName(), dopt);
  }

}
