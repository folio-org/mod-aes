package org.folio.aes.util;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.folio.aes.util.AesUtil;
import org.junit.Test;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;

public class AesUtilTest {

  private static final String jsonpathTestContent;
  static {
    try {
      Path path = Paths.get(AesUtilTest.class.getClassLoader().getResource("jsonpath.json").toURI());
      jsonpathTestContent = new String(Files.readAllBytes(path));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testHasJsonPathPositive() {
    String jsonpath = "$.store.book[?(@.price < 10)]";
    assertTrue(AesUtil.hasJsonPath(jsonpathTestContent, jsonpath));
  }

  @Test
  public void testHasJsonPathNegative() {
    String jsonpath = "$.store.book[?(@.price > 1000)]";
    assertFalse(AesUtil.hasJsonPath(jsonpathTestContent, jsonpath));
  }

  @Test
  public void testConvertMultiMapToJsonObject() {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add("a", "1 a");
    map.add("a", "2 a");
    map.add("a", "3 a");
    map.add("b", "1 b");
    JsonObject jo = AesUtil.convertMultiMapToJsonObject(map);
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
      AesUtil.maskPassword(afterMask);
      assertEquals(beforeMask.size(), afterMask.size());
      afterMask.forEach(entry -> {
        if (entry.getKey().contains("assword")) {
          assertNotEquals(entry.getValue().toString(), beforeMask.getString(entry.getKey()));
          assertEquals(Constant.PASSWORD_MASK, entry.getValue().toString());
        } else {
          assertEquals(entry.getValue().toString(), beforeMask.getString(entry.getKey()));
        }
      });
    }
  }

}
