package org.folio.aes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.folio.aes.model.RoutingRule;
import org.junit.jupiter.api.Test;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class AesUtilsTest {

  private static final String jsonpathTestContent;
  static {
    try {
      Path path = Paths.get(AesUtilsTest.class.getClassLoader().getResource("jsonpath.json").toURI());
      jsonpathTestContent = new String(Files.readAllBytes(path));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testCheckRoutingRules() {
    RoutingRule goodRoutingRule = new RoutingRule("$.store.book[?(@.price < 10)]", "tenant1");
    RoutingRule badRoutingRule = new RoutingRule("$.store.book[?(@.price > 100)]", "tenant2");
    Set<RoutingRule> routingRules = new HashSet<>(Arrays.asList(goodRoutingRule, badRoutingRule));
    Set<RoutingRule> rs = AesUtils.checkRoutingRules(jsonpathTestContent, routingRules);
    assertTrue(rs.contains(goodRoutingRule));
    assertFalse(rs.contains(badRoutingRule));
  }

  @Test
  void testConvertMultiMapToJsonObject() {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add("a", "1 a");
    map.add("a", "2 a");
    map.add("a", "3 a");
    map.add("b", "1 b");
    JsonObject jo = AesUtils.convertMultiMapToJsonObject(map);
    assertTrue(jo.getJsonArray("a").contains("1 a"));
    assertTrue(jo.getJsonArray("a").contains("2 a"));
    assertTrue(jo.getJsonArray("a").contains("3 a"));
    assertEquals("1 b", jo.getString("b"));
  }

  @Test
  void testMaskPassword() {
    String login = "{\"username\":\"admin\",\"password\":\"secret\"}";
    String update = "{\"username\":\"admin\",\"userId\":\"123\",\"password\":\"before\",\"newPassword\":\"after\",\"id\":\"456\"}";
    for (String s : Arrays.asList(login, update)) {
      Object beforeMask = new JsonObject(s);
      Object afterMask = new JsonObject(s);
      AesUtils.maskPassword(afterMask);
      assertEquals(((JsonObject) beforeMask).size(), ((JsonObject) afterMask).size());
      ((JsonObject) afterMask).forEach(entry -> {
        if (entry.getKey().contains("assword")) {
          assertNotEquals(entry.getValue().toString(),
              ((JsonObject) beforeMask).getString(entry.getKey()));
          assertEquals(AesConstants.MSG_PW_MASK, entry.getValue().toString());
        } else {
          assertEquals(entry.getValue().toString(),
              ((JsonObject) beforeMask).getString(entry.getKey()));
        }
      });
    }
  }

  @Test
  void testMaskPasswordWithArray() {
    String login = "[{\"username\":\"admin\",\"password\":\"secret\"}]";
    String update = "[{\"username\":\"admin\",\"userId\":\"123\",\"password\":\"before\",\"newPassword\":\"after\",\"id\":\"456\"}]";
    for (String s : Arrays.asList(login, update)) {
      Object beforeMask = new JsonArray(s);
      Object afterMask = new JsonArray(s);
      AesUtils.maskPassword(afterMask);
      assertEquals(((JsonArray) beforeMask).size(), ((JsonArray) afterMask).size());
      assertEquals(((JsonArray) beforeMask).getJsonObject(0).size(),
          ((JsonArray) afterMask).getJsonObject(0).size());
      ((JsonArray) afterMask).getJsonObject(0).forEach(entry -> {
        if (entry.getKey().contains("assword")) {
          assertNotEquals(entry.getValue().toString(),
              ((JsonArray) beforeMask).getJsonObject(0).getString(entry.getKey()));
          assertEquals(AesConstants.MSG_PW_MASK, entry.getValue().toString());
        } else {
          assertEquals(entry.getValue().toString(),
              ((JsonArray) beforeMask).getJsonObject(0).getString(entry.getKey()));
        }
      });
    }
  }

  @Test
  void testContainsPII() {
    final Object user = new JsonObject("{\"username\":\"jhandey\",\"id\":\"7261ecaae3a74dc68b468e12a70b1aec\",\"active\":true,\"type\":\"patron\",\"patronGroup\":\"4bb563d9-3f9d-4e1e-8d1d-04e75666d68f\",\"meta\":{\"creation_date\":\"2016-11-05T0723\",\"last_login_date\":\"\"},\"personal\":{\"lastName\":\"Handey\",\"firstName\":\"Jack\",\"email\":\"jhandey@biglibrary.org\",\"phone\":\"2125551212\"}}");

    assertTrue(AesUtils.containsPII(user));
  }

  @Test
  void testContainsPIINoPII() {
    final Object item = new JsonObject("{\"id\":\"0b96a642-5e7f-452d-9cae-9cee66c9a892\",\"title\":\"Uprooted\",\"callNumber\":\"D11.J54 A3 2011\",\"barcode\":\"645398607547\",\"status\":{\"name\":\"Available\"},\"materialType\":{\"id\":\"fcf3d3dc-b27f-4ce4-a530-542ea53cacb5\",\"name\":\"Book\"},\"permanentLoanType\":{\"id\":\"8e570d0d-931c-43d1-9ca1-221e693ea8d2\",\"name\":\"Can Circulate\"},\"temporaryLoanType\":{\"id\":\"74c25903-4019-4d8a-9360-5cb7761f44e5\",\"name\":\"Course Reserve\"},\"permanentLocation\":{\"id\":\"d9cd0bed-1b49-4b5e-a7bd-064b8d177231\",\"name\":\"Main Library\"}}");

    assertFalse(AesUtils.containsPII(item));
  }

  @Test
  void testContainsPIIWithJsonArray() {
    final Object user = new JsonArray("[{\"username\":\"jhandey\",\"id\":\"7261ecaae3a74dc68b468e12a70b1aec\",\"active\":true,\"type\":\"patron\",\"patronGroup\":\"4bb563d9-3f9d-4e1e-8d1d-04e75666d68f\",\"meta\":{\"creation_date\":\"2016-11-05T0723\",\"last_login_date\":\"\"},\"personal\":{\"lastName\":\"Handey\",\"firstName\":\"Jack\",\"email\":\"jhandey@biglibrary.org\",\"phone\":\"2125551212\"}}]");

    assertTrue(AesUtils.containsPII(user));
  }

  @Test
  void testContainsPIINoPIIWithJsonArray() {
    final Object item = new JsonArray("[{\"id\":\"0b96a642-5e7f-452d-9cae-9cee66c9a892\",\"title\":\"Uprooted\",\"callNumber\":\"D11.J54 A3 2011\",\"barcode\":\"645398607547\",\"status\":{\"name\":\"Available\"},\"materialType\":{\"id\":\"fcf3d3dc-b27f-4ce4-a530-542ea53cacb5\",\"name\":\"Book\"},\"permanentLoanType\":{\"id\":\"8e570d0d-931c-43d1-9ca1-221e693ea8d2\",\"name\":\"Can Circulate\"},\"temporaryLoanType\":{\"id\":\"74c25903-4019-4d8a-9360-5cb7761f44e5\",\"name\":\"Course Reserve\"},\"permanentLocation\":{\"id\":\"d9cd0bed-1b49-4b5e-a7bd-064b8d177231\",\"name\":\"Main Library\"}}]");

    assertFalse(AesUtils.containsPII(item));
  }

  @Test
  void testContainsPIIUserList() throws Exception {
    final JsonObject users = new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src/test/resources/data/users.json"))));

    assertTrue(AesUtils.containsPII(users));
  }

  @Test
  void testDecodeOkapiToken() {
    String token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkaWt1X3VzZXIiLCJ1c2VyX2lkIjoiYWJjIiwidGVuYW50IjoiZGlrdSJ9.eMu6_Gjjo6G6TeTS3y--GmQGTtWryJtKznpGUUwpa0rDDwY1xLBDTQoHv06_mXYs2GyPOoeERUM_G_BEvpMZcA";
    JsonObject jo = AesUtils.decodeOkapiToken(token);
    assertEquals("diku_user", jo.getValue("sub"));
    assertEquals("abc", jo.getValue("user_id"));
    assertEquals("diku", jo.getValue("tenant"));
  }

  @Test
  void testDecodeOkapiTokenInvalidTokenFomrat() {
    final String token = "abc";
    assertThrows(
        IllegalArgumentException.class,
        () -> AesUtils.decodeOkapiToken(token));
  }

  @Test
  void testDecodeOkapiTokenInvalidJson() {
    final String token = "abc.def";
    assertThrows(
        IllegalArgumentException.class,
        () -> AesUtils.decodeOkapiToken(token));
  }
}
