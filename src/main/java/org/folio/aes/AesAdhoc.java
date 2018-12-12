package org.folio.aes;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AesAdhoc {

  public static void main(String[] args) throws InterruptedException, ExecutionException {
     System.out.println(Thread.currentThread().getId());
     System.out.println(Thread.currentThread().getName());
     // Using Lambda Expression
     CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
     // Simulate a long-running Job
     try {
     TimeUnit.SECONDS.sleep(2);
     System.out.println(Thread.currentThread().getId());
     System.out.println(Thread.currentThread().getName());
     } catch (InterruptedException e) {
     throw new IllegalStateException(e);
     }
     System.out.println("I'll run in a separate thread than the main thread.");
     });
     Thread.sleep(5000);
     System.out.println("waking up");
     future.get();

//    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//      try {
//        TimeUnit.SECONDS.sleep(1);
//      } catch (InterruptedException e) {
//        throw new IllegalStateException(e);
//      }
//      return "Result of the asynchronous computation";
//    });
//    System.out.println(future.get());

  }

}
