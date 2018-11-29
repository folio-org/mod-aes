package org.folio.aes.util;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
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
          assertEquals(AesConstants.PASSWORD_MASK, entry.getValue().toString());
        } else {
          assertEquals(entry.getValue().toString(), beforeMask.getString(entry.getKey()));
        }
      });
    }
  }

  @Test
  public void testContainsPII() {
    String user = "{\"username\":\"jhandey\",\"id\":\"7261ecaae3a74dc68b468e12a70b1aec\",\"active\":true,\"type\":\"patron\",\"patronGroup\":\"4bb563d9-3f9d-4e1e-8d1d-04e75666d68f\",\"meta\":{\"creation_date\":\"2016-11-05T0723\",\"last_login_date\":\"\"},\"personal\":{\"lastName\":\"Handey\",\"firstName\":\"Jack\",\"email\":\"jhandey@biglibrary.org\",\"phone\":\"2125551212\"}}";

    assertTrue(AesUtil.containsPII(user));
  }

  @Test
  public void testContainsPIINoPII() {
    String item = "{\"id\":\"0b96a642-5e7f-452d-9cae-9cee66c9a892\",\"title\":\"Uprooted\",\"callNumber\":\"D11.J54 A3 2011\",\"barcode\":\"645398607547\",\"status\":{\"name\":\"Available\"},\"materialType\":{\"id\":\"fcf3d3dc-b27f-4ce4-a530-542ea53cacb5\",\"name\":\"Book\"},\"permanentLoanType\":{\"id\":\"8e570d0d-931c-43d1-9ca1-221e693ea8d2\",\"name\":\"Can Circulate\"},\"temporaryLoanType\":{\"id\":\"74c25903-4019-4d8a-9360-5cb7761f44e5\",\"name\":\"Course Reserve\"},\"permanentLocation\":{\"id\":\"d9cd0bed-1b49-4b5e-a7bd-064b8d177231\",\"name\":\"Main Library\"}}";

    assertFalse(AesUtil.containsPII(item));
  }

  @Test
  public void testContainsPIIUserList() throws Exception {
    String users = new String(Files.readAllBytes(Paths.get("src/test/resources/data/users.json")), StandardCharsets.UTF_8);

    assertTrue(AesUtil.containsPII(users));
  }
}
