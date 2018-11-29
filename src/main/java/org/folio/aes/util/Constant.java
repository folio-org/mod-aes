package org.folio.aes.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

public class Constant {

  private Constant() {
  }

  public static List<String> PASSWORDS = Arrays.asList("password", "newPassword");
  public static final String PASSWORD_MASK = "xxx";

  public static final String MOD_NAME = "mod-aes";
  public static final String OKAPI_URL = "x-okapi-url";
  public static final String OKAPI_TENANT = "x-okapi-tenant";
  public static final String OKAPI_TOKEN = "x-okapi-token";
  public static final String OKAPI_FILTER = "x-okapi-filter";

  public static final String PHASE = "PHASE";
  public static final String PHASE_PRE = "pre";
  public static final String PHASE_POST = "post";

  public static final String TENANT_DEFAULT = "no_tenant";
  public static final int BODY_LIMIT = 5000;

  public static final String ROUTING_CRITERIA = "criteria";
  public static final String ROUTING_TARGET = "target";

  public static final String CONFIG_ROUTING_QUREY = "/configurations/entries?query=(module=AES and configName=routing_rules)&limit=100";


  public static final Configuration PII_JSON_PATH_CONFIG = Configuration
      .defaultConfiguration()
      .addOptions(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS);
  public static final List<JsonPath> PII_JSON_PATHS =
      Collections.unmodifiableList(Arrays.asList(
            JsonPath.compile("$..personal"),
            JsonPath.compile("$..username"),
            JsonPath.compile("$..requester"),
            JsonPath.compile("$..user")
          ));
}
