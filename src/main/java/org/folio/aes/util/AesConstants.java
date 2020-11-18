package org.folio.aes.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

public class AesConstants {

  private AesConstants() {
  }

  public static final String MOD_NAME = "mod-aes";

  // mod-config related
  public static final String CONFIG_CONFIGS = "configs";
  public static final String CONFIG_VALUE = "value";
  public static final String CONFIG_QUERY_CQL =
      URLEncoder.encode("(module=AES and configName=routing_rules)", UTF_8);
  public static final String CONFIG_ROUTING_QUERY = String
    .format("/configurations/entries?query=%s&limit=%d", CONFIG_QUERY_CQL, Integer.MAX_VALUE);
  public static final String CONFIG_ROUTING_CRITERIA = "criteria";
  public static final String CONFIG_ROUTING_TARGET = "target";

  // okapi headers
  public static final String OKAPI_URL = "x-okapi-url";
  public static final String OKAPI_TENANT = "x-okapi-tenant";
  public static final String OKAPI_TOKEN = "x-okapi-token";
  public static final String OKAPI_TOKEN_SUB = "sub";
  public static final String OKAPI_TOKEN_AUTH_MOD = "_AUTHZ_MODULE_";
  public static final String OKAPI_AES_FILTER_ID = "x-okapi-aes-filter-id";
  public static final String OKAPI_HANDLER_RESULT = "x-okapi-handler-result";
  public static final String CONTENT_TYPE = "content-type";

  // topics
  public static final String TENANT_NONE = "tenant_none";

  // message related JSON keys
  public static final String MSG_PATH = "path";
  public static final String MSG_HEADERS = "headers";
  public static final String MSG_PARAMS = "params";
  public static final String MSG_BODY = "body";
  public static final String MSG_BODY_CONTENT = "content";
  public static final String MSG_PII = "pii";

  // message password masking
  @SuppressWarnings({"squid:S2068"})
  public static final List<String> MSG_PW = Collections.unmodifiableList(Arrays.asList("password", "newPassword"));
  public static final String MSG_PW_MASK = "xxx";

  public static final Configuration PII_JSON_PATH_CONFIG = Configuration
    .defaultConfiguration()
    .addOptions(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS);

  // list of JSON paths that contain personal info
  public static final List<JsonPath> PII_JSON_PATHS = Collections.unmodifiableList(Arrays.asList(
    JsonPath.compile("$..personal"),
    JsonPath.compile("$..username"),
    JsonPath.compile("$..requester"),
    JsonPath.compile("$..user")));
}
