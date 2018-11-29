package org.folio.aes.util;

import java.util.Arrays;
import java.util.List;

public class AesConstants {

  private AesConstants() {
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
  public static final int BODY_LIMIT = 10000;

  public static final String ROUTING_CRITERIA = "criteria";
  public static final String ROUTING_TARGET = "target";

  public static final String CONFIG_ROUTING_QUREY = "/configurations/entries?query=(module=AES and configName=routing_rules)&limit=100";

  public static final String AES_FILTER_ID = "x-okapi-aes-filter-id";

}
