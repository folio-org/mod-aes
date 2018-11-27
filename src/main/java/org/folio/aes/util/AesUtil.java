package org.folio.aes.util;

import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AesUtil {

  private AesUtil() {
  }

  // adjust JsonPath configuration to simplify JsonPath checking
  private static final Configuration jpConfig = Configuration.defaultConfiguration()
    .addOptions(Option.SUPPRESS_EXCEPTIONS).addOptions(Option.ALWAYS_RETURN_LIST);

  /**
   * Given a list of JsonPaths, check if given json contains each.
   *
   * @param json
   * @param jsonPaths
   * @return
   */
  public static List<Boolean> hasJsonPath(String json, List<String> jsonPaths) {
    ReadContext ctx = JsonPath.using(jpConfig).parse(json);
    List<Boolean> rs = new ArrayList<>(jsonPaths.size());
    for (String jsonPath : jsonPaths) {
      List<?> list = ctx.read(jsonPath);
      rs.add(!list.isEmpty());
    }
    return rs;
  }

  /**
   * Convert {@link MultiMap} to {@link JsonObject}. Collapse values with same key
   * to an array.
   *
   * @param multiMap
   * @return
   */
  public static JsonObject convertMultiMapToJsonObject(MultiMap multiMap) {
    JsonObject jo = new JsonObject();
    multiMap.forEach(entry -> {
      String key = entry.getKey();
      String value = entry.getValue();
      if (jo.containsKey(key)) {
        Object obj = jo.getValue(key);
        if (obj instanceof JsonArray) {
          ((JsonArray) obj).add(value);
        } else {
          jo.put(key, new JsonArray().add(obj).add(value));
        }
      } else {
        jo.put(key, value);
      }
    });
    return jo;
  }

  /**
   * Mask password.
   *
   * @param jsonObject
   */
  public static void maskPassword(JsonObject jsonObject) {
    for (String s : Constant.PASSWORDS) {
      if (jsonObject.containsKey(s)) {
        jsonObject.put(s, Constant.PASSWORD_MASK);
      }
    }
  }

  public static boolean containsPII(String bodyString) {
    for (final JsonPath jp : Constant.PII_JSON_PATHS) {
      final List<String> pathList = jp.read(bodyString,
          Constant.PII_JSON_PATH_CONFIG);
      if (!pathList.isEmpty()) {
        return true;
      }
    }

    return false;
  }
}
