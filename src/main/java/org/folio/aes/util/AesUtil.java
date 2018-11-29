package org.folio.aes.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
   * Check JsonPaths.
   *
   * @param json
   * @param jsonPaths
   */
  public static Set<String> checkJsonPaths(String json, Set<String> jsonPaths) {
    Set<String> goodOnes = new HashSet<>();
    ReadContext ctx = JsonPath.using(jpConfig).parse(json);
    jsonPaths.forEach(jsonPath -> {
      List<?> list = ctx.read(jsonPath);
      if (!list.isEmpty()) {
        goodOnes.add(jsonPath);
      }
    });
    return goodOnes;
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

}
