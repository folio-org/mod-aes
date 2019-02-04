package org.folio.aes.service;

import static org.awaitility.Awaitility.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static org.folio.aes.util.AesConstants.*;
import static org.junit.Assert.*;

import org.folio.aes.model.RoutingRule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class AesServiceTest {

  private static long WAIT_TS = 1000;

  @Mock
  private RuleService ruleService;

  @Mock
  private QueueService queueService;

  @Mock
  private RoutingContext ctx;

  @Mock
  private HttpServerRequest request;

  @Mock
  private HttpServerResponse response;

  @InjectMocks
  private AesService aesService;

  @Before
  public void setUp() {
    aesService = new AesService();
    MockitoAnnotations.initMocks(this);
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
  }

  @Test
  public void testStop() throws Exception {
    aesService.stop();
    verify(queueService, times(1)).stop();
  }

  @Test
  public void testSendSkipFilterId() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_AES_FILTER_ID, "true");
    when(request.headers()).thenReturn(headers);
    aesService.prePostHandler(ctx);
    verifyZeroInteractions(ruleService);
    verifyZeroInteractions(queueService);
  }

  @Test
  public void testSendSkipBadToken() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TOKEN, "abc");
    when(request.headers()).thenReturn(headers);
    aesService.prePostHandler(ctx);
    verifyZeroInteractions(ruleService);
    verifyZeroInteractions(queueService);
  }

  @Test
  public void testSendSkipInternalAuth() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TOKEN,
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJfQVVUSFpfTU9EVUxFXyJ9.18R_RlVJNlrDRynNEbGSRlf3E51QKYyaZOjwr9tVQmYUitJS5Ps_EeU3JVHZRrDoNnRLQGmHZwiNxMYEBBDKQQ");
    when(request.headers()).thenReturn(headers);
    aesService.prePostHandler(ctx);
    verifyZeroInteractions(ruleService);
    verifyZeroInteractions(queueService);
  }

  @Test
  public void testSendTenantNone() throws InterruptedException {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Content-Type", "application/json");
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    aesService.prePostHandler(ctx);
    long start = System.currentTimeMillis() + WAIT_TS;
    await().until(() -> {
      return System.currentTimeMillis() > start;
    });
    verifyZeroInteractions(ruleService);
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(queueService, times(1)).send(eq(TENANT_NONE), argument.capture());
    JsonObject msg = new JsonObject(argument.getValue());
    assertFalse(msg.getBoolean(MSG_PII));
    assertTrue(msg.getJsonObject(MSG_BODY).containsKey(MSG_BODY_CONTENT));
    assertTrue(msg.containsKey(MSG_PATH));
    assertTrue(msg.containsKey(MSG_HEADERS));
    assertTrue(msg.containsKey(MSG_PARAMS));
  }

  @Test
  public void testSendRuleException() throws InterruptedException {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TENANT, "abc");
    headers.add("Content-Type", "application/json");
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    CompletableFuture<Collection<RoutingRule>> cf = new CompletableFuture<>();
    cf.completeExceptionally(new RuntimeException("abc"));
    when(ruleService.getRules(any(), any(), any())).thenReturn(cf);
    aesService.prePostHandler(ctx);
    long start = System.currentTimeMillis() + WAIT_TS;
    await().until(() -> {
      return System.currentTimeMillis() > start;
    });
    verify(ruleService, times(1)).getRules(any(), any(), any());
    verifyZeroInteractions(queueService);
  }

  @Test
  public void testSendWithoutRules() throws InterruptedException {
    String defaultTopic = "abc_default";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN,
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    headers.add("Content-Type", "application/json");
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBodyAsString()).thenReturn("{}");
    CompletableFuture<Collection<RoutingRule>> cf = new CompletableFuture<>();
    cf.complete(new ArrayList<>());
    when(ruleService.getRules(any(), any(), any())).thenReturn(cf);
    aesService.prePostHandler(ctx);
    long start = System.currentTimeMillis() + WAIT_TS;
    await().until(() -> {
      return System.currentTimeMillis() > start;
    });
    verify(ruleService, times(1)).getRules(any(), any(), any());
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(queueService).send(eq(defaultTopic), argument.capture());
    checkMsg(argument.getValue());
    verify(queueService).send(eq(defaultTopic), argument.capture());
    checkMsg(argument.getValue());
    verifyNoMoreInteractions(queueService);
  }

  @Test
  public void testSend() throws InterruptedException {
    String topicA = "ta";
    String topicB = "tb";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN,
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    headers.add("Content-Type", "application/json");
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBodyAsString()).thenReturn("{}");
    Collection<RoutingRule> rules = new ArrayList<>();
    rules.add(new RoutingRule("$..*", topicA));
    rules.add(new RoutingRule("$..*", topicB));
    rules.add(new RoutingRule("$.xyz", topicB));
    CompletableFuture<Collection<RoutingRule>> cf = new CompletableFuture<>();
    cf.complete(rules);
    when(ruleService.getRules(any(), any(), any())).thenReturn(cf);
    aesService.prePostHandler(ctx);
    long start = System.currentTimeMillis() + WAIT_TS;
    await().until(() -> {
      return System.currentTimeMillis() > start;
    });
    verify(ruleService, times(1)).getRules(any(), any(), any());
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(queueService).send(eq(topicA), argument.capture());
    checkMsg(argument.getValue());
    verify(queueService).send(eq(topicB), argument.capture());
    checkMsg(argument.getValue());
    verifyNoMoreInteractions(queueService);
  }

  private static void checkMsg(String content) {
    JsonObject msg = new JsonObject(content);
    assertTrue(msg.containsKey(MSG_PATH));
    assertTrue(msg.containsKey(MSG_HEADERS));
    assertTrue(msg.containsKey(MSG_PARAMS));
    assertTrue(msg.containsKey(MSG_PII));
    assertTrue(msg.containsKey(MSG_BODY));
  }

}
