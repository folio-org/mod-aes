package org.folio.aes.util;

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
   * Check if given Json has a JsonPath.
   *
   * @param json
   * @param jsonPath
   *
   * @return true if has, false otherwise
   */
  public static boolean hasJsonPath(String json, String jsonPath) {
    ReadContext ctx = JsonPath.using(jpConfig).parse(json);
    List<?> rs = ctx.read(jsonPath);
    return !rs.isEmpty();
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
