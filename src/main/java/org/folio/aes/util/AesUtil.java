package org.folio.aes.util;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AesUtil {

  private AesUtil() {
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
   * Mask password
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
