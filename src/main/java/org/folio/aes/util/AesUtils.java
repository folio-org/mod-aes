package org.folio.aes.util;

import static org.folio.aes.util.AesConstants.MSG_PW;
import static org.folio.aes.util.AesConstants.MSG_PW_MASK;
import static org.folio.aes.util.AesConstants.PII_JSON_PATHS;
import static org.folio.aes.util.AesConstants.PII_JSON_PATH_CONFIG;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.ReadContext;
import io.vertx.core.MultiMap;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.folio.aes.model.RoutingRule;


public class AesUtils {

  private AesUtils() {
  }

  // adjust JsonPath configuration to simplify path checking
  private static final Configuration jpConfig = Configuration.defaultConfiguration()
      .addOptions(Option.SUPPRESS_EXCEPTIONS, Option.ALWAYS_RETURN_LIST);
  private static final ParseContext parseContext = JsonPath.using(jpConfig);

  /**
   * Check routing rules.
   *
   * @param json JSON to check
   * @param routingRules rules to apply to the JSON
   */
  public static Set<RoutingRule> checkRoutingRules(String json,
      Collection<RoutingRule> routingRules) {
    Set<RoutingRule> goodOnes = new HashSet<>();
    ReadContext ctx = parseContext.parse(json);
    routingRules.forEach(routingRule -> {
      List<?> list = ctx.read(routingRule.getCriteria());
      if (!list.isEmpty()) {
        goodOnes.add(routingRule);
      }
    });
    return goodOnes;
  }

  /**
   * Convert {@link MultiMap} to {@link JsonObject}. Collapse values with same key
   * to an array.
   *
   * @param multiMap map to convert to JsonObject
   * @return JSON object containing the multi map data
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
   * @param o either a JsonObject of JsonArray that may contain password fields
   */
  public static void maskPassword(Object o) {
    if (o instanceof JsonObject) {
      maskPassword((JsonObject) o);
    } else {
      for (Object child : (JsonArray) o) {
        maskPassword(child);
      }
    }
  }

  /**
   * Mask password.
   *
   * @param jsonObject JSON to check for password data
   */
  public static void maskPassword(JsonObject jsonObject) {
    for (String s : MSG_PW) {
      if (jsonObject.containsKey(s)) {
        jsonObject.put(s, MSG_PW_MASK);
      }
    }
  }

  /**
   * Determine whether or not the passed in object contains personal data (PII).
   *
   * @param o either a JsonObject or a JsonArray to be checked for personal data
   * @return whether or not personal data was found
   */
  public static boolean containsPersonalData(Object o) {
    if (o instanceof JsonObject) {
      return containsPersonalData((JsonObject) o);
    } else {
      for (Object child : (JsonArray) o) {
        if (containsPersonalData(child)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Determine whether or not the passed in JsonObject contains personal data (PII).
   *
   * @param body JSON to be checked for personal data
   * @return whether or not personal data was found
   */
  public static boolean containsPersonalData(JsonObject body) {
    for (final JsonPath jp : PII_JSON_PATHS) {
      final List<String> pathList = jp.read(body.toString(), PII_JSON_PATH_CONFIG);
      if (!pathList.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Decode Okapi token to {@link JsonObject}.
   *
   * @param okapiToken the Okapi token
   * @return the decoded token data
   */
  public static JsonObject decodeOkapiToken(String okapiToken) {
    String encodedJson;
    try {
      encodedJson = okapiToken.split("\\.")[1];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    String decodedJson = new String(Base64.getDecoder().decode(encodedJson));
    try {
      return new JsonObject(decodedJson);
    } catch (DecodeException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

}
