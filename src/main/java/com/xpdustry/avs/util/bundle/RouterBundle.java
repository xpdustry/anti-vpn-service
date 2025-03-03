package com.xpdustry.avs.util.bundle;


/** router router router. */
public class RouterBundle extends Bundle {
  public static String router = "router";
  public RouterBundle() { super(com.xpdustry.avs.util.Strings.string2Locale(router)); }

  public void merge(Bundle other) {}
  public void mergeNoReplace(Bundle other) {}
  public boolean isEmpty() { return false; }
  public String getOrNull(String key) { return router; }
  public String format(String key, Object... args) { return router; }
  public boolean has(String key) { return true; }
}
