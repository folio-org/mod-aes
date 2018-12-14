package org.folio.aes.util;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.folio.aes.util.AesUtils;
import org.junit.Test;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;

public class AesUtilsTest {

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
  public void testCheckJsonPaths() {
    String goodJsonPath = "$.store.book[?(@.price < 10)]";
    String badJsonPath = "$.store.book[?(@.price > 100)]";
    Set<String> jsonPaths = new HashSet<>(Arrays.asList(goodJsonPath, badJsonPath));
    Set<String> rs = AesUtils.checkJsonPaths(jsonpathTestContent, jsonPaths);
    assertTrue(rs.contains(goodJsonPath));
    assertFalse(rs.contains(badJsonPath));
  }

  @Test
  public void testConvertMultiMapToJsonObject() {
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
  public void testMaskPassword() {
    String login = "{\"username\":\"admin\",\"password\":\"secret\"}";
    String update = "{\"username\":\"admin\",\"userId\":\"123\",\"password\":\"before\",\"newPassword\":\"after\",\"id\":\"456\"}";
    for (String s : Arrays.asList(login, update)) {
      JsonObject beforeMask = new JsonObject(s);
      JsonObject afterMask = new JsonObject(s);
      AesUtils.maskPassword(afterMask);
      assertEquals(beforeMask.size(), afterMask.size());
      afterMask.forEach(entry -> {
        if (entry.getKey().contains("assword")) {
          assertNotEquals(entry.getValue().toString(), beforeMask.getString(entry.getKey()));
          assertEquals(AesConstants.MSG_PASSWORD_MASK, entry.getValue().toString());
        } else {
          assertEquals(entry.getValue().toString(), beforeMask.getString(entry.getKey()));
        }
      });
    }
  }

  @Test
  public void testDecodeOkapiToken() {
    String token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkaWt1X3VzZXIiLCJ1c2VyX2lkIjoiYWJjIiwidGVuYW50IjoiZGlrdSJ9.eMu6_Gjjo6G6TeTS3y--GmQGTtWryJtKznpGUUwpa0rDDwY1xLBDTQoHv06_mXYs2GyPOoeERUM_G_BEvpMZcA";
    JsonObject jo = AesUtils.decodeOkapiToken(token);
    assertEquals("diku_user", jo.getValue("sub"));
    assertEquals("abc", jo.getValue("user_id"));
    assertEquals("diku", jo.getValue("tenant"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDecodeOkapiTokenInvalidTokenFomrat() {
    String token = "abc";
    AesUtils.decodeOkapiToken(token);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDecodeOkapiTokenInvalidJson() {
    String token = "abc.def";
    AesUtils.decodeOkapiToken(token);
  }

}
