package org.folio.aes.service;

import static org.awaitility.Awaitility.await;
import static org.folio.aes.util.AesConstants.MSG_BODY;
import static org.folio.aes.util.AesConstants.MSG_BODY_CONTENT;
import static org.folio.aes.util.AesConstants.MSG_HEADERS;
import static org.folio.aes.util.AesConstants.MSG_PARAMS;
import static org.folio.aes.util.AesConstants.MSG_PATH;
import static org.folio.aes.util.AesConstants.MSG_PII;
import static org.folio.aes.util.AesConstants.OKAPI_AES_FILTER_ID;
import static org.folio.aes.util.AesConstants.OKAPI_HANDLER_RESULT;
import static org.folio.aes.util.AesConstants.OKAPI_TENANT;
import static org.folio.aes.util.AesConstants.OKAPI_TOKEN;
import static org.folio.aes.util.AesConstants.TENANT_NONE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.folio.aes.model.RoutingRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AesServiceTest {

  private static long WAIT_TS = 1000;

  @Mock
  private RuleService ruleService;

  @Mock
  private QueueService queueService;

  @InjectMocks
  private AesService aesService;

  @Test
  void testStop() throws Exception {
    aesService.stop();
    verify(queueService, times(1)).stop();
  }

  @Test
  void testSendSkipFilterId(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_AES_FILTER_ID, "true");
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    aesService.prePostHandler(ctx);
    verifyNoMoreInteractions(ruleService);
    verifyNoMoreInteractions(queueService);
  }

  @Test
  void testSendSkipBadToken(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TOKEN, "abc");
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    aesService.prePostHandler(ctx);
    verifyNoMoreInteractions(ruleService);
    verifyNoMoreInteractions(queueService);
  }

  @Test
  void testSendSkipInternalAuth(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJfQVVUSFpfTU9EVUxFXyJ9."
        + "18R_RlVJNlrDRynNEbGSRlf3E51QKYyaZOjwr9tVQmYUitJS5Ps_EeU3JVHZRrDoNnRLQGmHZwiNxMYEBBDKQQ");
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    aesService.prePostHandler(ctx);
    verifyNoMoreInteractions(ruleService);
    verifyNoMoreInteractions(queueService);
  }

  @Test
  void testSendTenantNone(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Content-Type", "application/json");
    headers.add(OKAPI_HANDLER_RESULT, "200");
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    aesService.prePostHandler(ctx);
    long start = System.currentTimeMillis() + WAIT_TS;
    await().until(() -> {
      return System.currentTimeMillis() > start;
    });
    verifyNoMoreInteractions(ruleService);
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
  void testSendRuleException(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TENANT, "abc");
    headers.add("Content-Type", "application/json");
    headers.add(OKAPI_HANDLER_RESULT, "200");
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    Promise<Collection<RoutingRule>> promise = Promise.promise();
    promise.fail("abc");
    when(ruleService.getRules(any(), any(), any())).thenReturn(promise.future());
    aesService.prePostHandler(ctx);
    long start = System.currentTimeMillis() + WAIT_TS;
    await().until(() -> {
      return System.currentTimeMillis() > start;
    });
    verify(ruleService, times(1)).getRules(any(), any(), any());
    verifyNoMoreInteractions(queueService);
  }

  @Test
  void testSendWithoutRules(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    final String defaultTopic = "abc_default";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-"
        + "qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    headers.add("Content-Type", "application/json");
    headers.add(OKAPI_HANDLER_RESULT, "200");
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBody()).thenReturn(Buffer.buffer("{}"));
    Promise<Collection<RoutingRule>> promise = Promise.promise();
    promise.complete(new ArrayList<>());
    when(ruleService.getRules(any(), any(), any())).thenReturn(promise.future());
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
  void testSend(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    final String topicA = "ta";
    final String topicB = "tb";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-"
        + "qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    headers.add("Content-Type", "application/json");
    headers.add(OKAPI_HANDLER_RESULT, "200");
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBody()).thenReturn(Buffer.buffer("{}"));
    Collection<RoutingRule> rules = List.of(
        new RoutingRule("$..*", topicA),
        new RoutingRule("$..*", topicB),
        new RoutingRule("$.xyz", topicB));
    Promise<Collection<RoutingRule>> promise = Promise.promise();
    promise.complete(rules);
    when(ruleService.getRules(any(), any(), any())).thenReturn(promise.future());
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

  @Test
  void testSendWithoutContentType(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    final String topicA = "ta";
    final String topicB = "tb";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-"
        + "qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    headers.add(OKAPI_HANDLER_RESULT, "200");
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBody()).thenReturn(Buffer.buffer());
    Collection<RoutingRule> rules = List.of(
        new RoutingRule("$..*", topicA),
        new RoutingRule("$..*", topicB),
        new RoutingRule("$.xyz", topicB));
    Promise<Collection<RoutingRule>> promise = Promise.promise();
    promise.complete(rules);
    when(ruleService.getRules(any(), any(), any())).thenReturn(promise.future());
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

  @Test
  void testSendWithError(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    final String topicA = "ta";
    final String topicB = "tb";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-"
        + "qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    headers.add(OKAPI_HANDLER_RESULT, "400");
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBody()).thenReturn(Buffer.buffer("This is an error"));
    Collection<RoutingRule> rules = List.of(
        new RoutingRule("$..*", topicA),
        new RoutingRule("$..*", topicB),
        new RoutingRule("$.xyz", topicB));
    Promise<Collection<RoutingRule>> promise = Promise.promise();
    promise.complete(rules);
    when(ruleService.getRules(any(), any(), any())).thenReturn(promise.future());
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

  @Test
  void testSendWithBadJson(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    final String topicA = "ta";
    final String topicB = "tb";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Content-Type", "application/json");
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-"
        + "qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    headers.add(OKAPI_HANDLER_RESULT, "200");
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBody()).thenReturn(Buffer.buffer("This is not JSON"));
    Collection<RoutingRule> rules = List.of(
        new RoutingRule("$..*", topicA),
        new RoutingRule("$..*", topicB),
        new RoutingRule("$.xyz", topicB));
    Promise<Collection<RoutingRule>> promise = Promise.promise();
    promise.complete(rules);
    when(ruleService.getRules(any(), any(), any())).thenReturn(promise.future());
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

  @Test
  void testSendWith422Error(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    final String topicA = "ta";
    final String topicB = "tb";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Content-Type", "application/json");
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-"
        + "qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    headers.add(OKAPI_HANDLER_RESULT, "422");
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBody()).thenReturn(Buffer.buffer("{\"error\":\"test\"}"));
    Collection<RoutingRule> rules = List.of(
        new RoutingRule("$..*", topicA),
        new RoutingRule("$..*", topicB),
        new RoutingRule("$.xyz", topicB));
    Promise<Collection<RoutingRule>> promise = Promise.promise();
    promise.complete(rules);
    when(ruleService.getRules(any(), any(), any())).thenReturn(promise.future());
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

  @ParameterizedTest
  @ValueSource(strings = {"200", "100", "garbage"})
  void testSendWithNoBodyAndVariousStatuses(String statusCode,
      @Mock RoutingContext ctx,
      @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    final String topicA = "ta";
    final String topicB = "tb";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Content-Type", "application/json");
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-"
        + "qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    headers.add(OKAPI_HANDLER_RESULT, statusCode);
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBody()).thenReturn(Buffer.buffer());
    Collection<RoutingRule> rules = List.of(
        new RoutingRule("$..*", topicA),
        new RoutingRule("$..*", topicB),
        new RoutingRule("$.xyz", topicB));
    Promise<Collection<RoutingRule>> promise = Promise.promise();
    promise.complete(rules);
    when(ruleService.getRules(any(), any(), any())).thenReturn(promise.future());
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

  @Test
  void testSendWithNoRestultStatusCode(@Mock RoutingContext ctx, @Mock HttpServerRequest request,
      @Mock HttpServerResponse response) throws InterruptedException {
    final String topicA = "ta";
    final String topicB = "tb";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Content-Type", "application/json");
    headers.add(OKAPI_TENANT, "abc");
    headers.add(OKAPI_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhYmMifQ.GHKsHPMokpfAhXrkrmA-"
        + "qxGEWsCreg2PwOTQUfc4tB8xqDufyR0MApWwwPODD52P86RYZYctrOvX6UBW8NOG5g");
    when(ctx.vertx()).thenReturn(Vertx.vertx());
    when(ctx.request()).thenReturn(request);
    when(ctx.response()).thenReturn(response);
    when(request.headers()).thenReturn(headers);
    when(request.params()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    when(ctx.getBody()).thenReturn(Buffer.buffer("{}"));
    Collection<RoutingRule> rules = List.of(
        new RoutingRule("$..*", topicA),
        new RoutingRule("$..*", topicB),
        new RoutingRule("$.xyz", topicB));
    Promise<Collection<RoutingRule>> promise = Promise.promise();
    promise.complete(rules);
    when(ruleService.getRules(any(), any(), any())).thenReturn(promise.future());
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
