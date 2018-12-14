package org.folio.aes.util;

import java.util.Arrays;
import java.util.List;

public class AesConstants {

  private AesConstants() {
  }

  public static final String MOD_NAME = "mod-aes";

  // mod-config related
  public static final String CONFIG_CONFIGS = "configs";
  public static final String CONFIG_VALUE = "value";
  public static final String CONFIG_ROUTING_QUREY = String
    .format("/configurations/entries?query=(module=AES and configName=routing_rules)&limit=%d", Integer.MAX_VALUE);
  public static final String CONFIG_ROUTING_CRITERIA = "criteria";
  public static final String CONFIG_ROUTING_TARGET = "target";

  // okapi headers
  public static final String OKAPI_URL = "x-okapi-url";
  public static final String OKAPI_TENANT = "x-okapi-tenant";
  public static final String OKAPI_TOKEN = "x-okapi-token";
  public static final String OKAPI_FILTER = "x-okapi-filter";
  public static final String OKAPI_TOKEN_SUB = "sub";
  public static final String OKAPI_TOKEN_AUTH_MOD = "_AUTHZ_MODULE_";
  public static final String OKAPI_AES_FILTER_ID = "x-okapi-aes-filter-id";

  // topics
  public static final String TENANT_NONE = "tenant_none";

  // message related JSON keys
  public static final String MSG_PATH = "path";
  public static final String MSG_HEADERS = "headers";
  public static final String MSG_PARAMS = "params";
  public static final String MSG_BODY = "body";
  public static final String MSG_BODY_CONTENT = "content";

  // message body content size limit
  public static final int MSG_BODY_CONTENT_LIMIT = 10000;

  // message password masking
  public static List<String> MSG_PASSWORDS = Arrays.asList("password", "newPassword");
  public static final String MSG_PASSWORD_MASK = "xxx";

}
