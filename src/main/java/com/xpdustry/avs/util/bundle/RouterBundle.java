package com.xpdustry.avs.util.bundle;


/** router router router. */
public class RouterBundle extends Bundle {
  @SuppressWarnings("deprecation")
  public RouterBundle() { super(new java.util.Locale("router")); }

  public void merge(Bundle other) {}
  public void mergeNoReplace(Bundle other) {}
  public boolean isEmpty() { return false; }
  public String getOrNull(String key) { return "router".intern(); }
  public String format(String key, Object... args) { return "router".intern(); }
  public boolean has(String key) { return true; }
}
