package org.folio.aes.util;

import java.util.Arrays;
import java.util.List;

public class Constant {

  private Constant() {
  }

  public static List<String> PASSWORDS = Arrays.asList("password", "newPassword");
  public static final String PASSWORD_MASK = "xxx";

  public static final String MOD_NAME = "mod-aes";
  public static final String OKAPI_TENANT = "x-okapi-tenant";
  public static final String OKAPI_FILTER = "x-okapi-filter";

  public static final String PHASE = "PHASE";
  public static final String PHASE_PRE = "pre";
  public static final String PHASE_POST = "post";

  public static final String TENANT_DEFAULT = "no_tenant";
  public static final int BODY_LIMIT = 5000;
}
