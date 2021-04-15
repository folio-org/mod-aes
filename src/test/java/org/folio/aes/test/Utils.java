package org.folio.aes.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test utils.
 */
public final class Utils {
  private Utils() {
    super();
  }

  /**
   * Retrieve a free port from the ephemeral range.
   *
   * @return the next port that is not likely used in the ephemeral range
   */
  public static int nextFreePort() {
    int maxTries = 10_000;
    do {
      final int port = ThreadLocalRandom.current().nextInt(49152, 65535);
      if (isLocalPortFree(port)) {
        return port;
      }
      if (--maxTries == 0) {
        return 8081;
      }
    } while (true);
  }

  private static boolean isLocalPortFree(int port) {
    try {
      new ServerSocket(port).close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
