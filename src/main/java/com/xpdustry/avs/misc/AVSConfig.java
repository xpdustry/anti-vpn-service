/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xpdustry.avs.misc;

import arc.Core;
import arc.files.Fi;
import arc.func.Func2;
import arc.struct.Seq;

import com.xpdustry.avs.util.DynamicSettings;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.Strings;


public class AVSConfig {
  public static final Seq<Field> all = new Seq<>();
  protected static Fi workingDirectory;
  protected static DynamicSettings config;  
  protected static boolean isLoading = false;
  protected static Logger logger = new Logger("Config");
  
  public static final Field
    enabled = new Field("enabled", true),
    defaultLocale = new Field("default-locale", "en", AVSConfigEvents::onDefaultLocaleChanged),
    kickMessage = new Field("kick-message", ""),
    serverBusyMessage = new Field("busy-message", ""),
    errorMessage = new Field("error-message", ""),
    clientKickDuration = new Field("kick-duration", 30),
    connectLimit = new Field("connect-limit", arc.util.OS.cores, AVSConfigEvents::onConnectLimitChanged),
    tokenCheckTimeout = new Field("token-timeout", 100),
    serviceCheckTimeout = new Field("service-timeout", 30),
    startupDownload = new Field("startup-download", true),
    autosaveSpacing = new Field("autosave-spacing", 60 * 15, AVSConfigEvents::onAutosaveSpacingChanged),
    resetCommandEnabled = new Field("reset-command", false, false, true),
    restrictedSettings = new Field("restricted-settings", String.join(",", connectLimit.name, kickMessage.name, serverBusyMessage.name, errorMessage.name), AVSConfigEvents::onRestrictedSettingsChanged),
    restrictedProviders = new Field("restricted-providers", String.join(",", "whitelist", "blacklist"), AVSConfigEvents::onRestrictedProvidersChanged),
    useDefaultBundle = new Field("bundle-default", com.xpdustry.avs.util.bundle.L10NBundle.useDefaultWhenKeyNotFound, AVSConfigEvents::onUseDefaultBundleChanged),
    randomOnlineProviders = new Field("random-online", false),
    randomTokens = new Field("random-tokens", false),
  //  xxx = new Field("???", true),
    resultRequired = new Field("result-required", true),
    
    
    // For devs, better to keep that as default
    pluginDirectory = new Field("plugin-dir", "", AVSConfigEvents::onPluginDirectoryChanged, true),
    bundlesDirectory = new Field("bundles-dir", "bundles", AVSConfigEvents::onBundlesDirectoryChanged, true),
    cacheDirectory = new Field("cache-dir", "cache", AVSConfigEvents::onCacheDirectoryChanged, true),
    cloudDirectory = new Field("cloud-dir", cacheDirectory.defaultValue + "/cloud", AVSConfigEvents::onCloudDirectoryChanged, true),
    settingsDirectory = new Field("settings-dir", "settings", AVSConfigEvents::onSettingsDirectoryChanged, true),
    tokensDirectory = new Field("tokens-dir", settingsDirectory.defaultValue + "/tokens", AVSConfigEvents::onTokensDirectoryChanged, true), 
    configFile = new Field("config-file", "config.json", AVSConfigEvents::onConfigFileChanged, true),
    defaultCacheFileExt = new Field("cache-ext", "bin", true),
    allowUntrustedSource = new Field("allow-http203", com.xpdustry.avs.util.network.AdvancedHttp.allowUntrustedSourceHttpCode, AVSConfigEvents::onAllowUntrustedSourceChanged, true),
    socketTimeout = new Field("socket-timeout", com.xpdustry.avs.util.network.AwaitHttp.readWriteTimeout, AVSConfigEvents::onSocketTimeoutChanged, true),
    useBundleCache = new Field("bundle-cache", com.xpdustry.avs.util.bundle.L10NBundle.useCache, AVSConfigEvents::onBundleCacheChanged, true),
    none = null
    ;

    
  public static Field get(String name) {
    return all.find(f -> f.name.equals(name));
  }
  
  protected static void setAllToDefault() {
    all.each(s -> config.put(s.name, s.defaultValue));
  }
  
  public static void notifyAllValueChanged() {
    all.each(s -> s.changed.get(s.get(), logger));
  }
    
  public static boolean isLoaded() {
    return config != null && !isLoading;
  }
  
  public static Fi subDir(String path) {
    // use the custom plugin directory first
    if (config != null || isLoading) {
      
      String pd =  pluginDirectory.getString(); //Core.settings.getString("avs-" + pluginDirectory.name, pluginDirectory.getString());
      if (!pd.isBlank()) setWorkingDirectory(Strings.getFiChild(Core.settings.getDataDirectory(), pd));
    }
    // By default, use the mods directory
    if (workingDirectory == null) setWorkingDirectory(mindustry.Vars.modDirectory);
    
    return Strings.getFiChild(workingDirectory, path);
  }
  
  public static Fi subDir(Fi path) {
    return subDir(path.path());
  }
  
  public static void setWorkingDirectory(Fi wd) {
    if (isLoaded()) throw new IllegalStateException("cannot change working directory after loading config");
    workingDirectory = wd;
  }
  
  public static Fi getWorkingDirectory() {
    return workingDirectory;
  }
  
  public static void load() {
    isLoading = true;
    
    //loadWithFileRedirection();
    loadWithFileRedirection();
    
    //setAllToDefault();
    //notifyAllValueChanged();
    
    isLoading = false;
  }
  
  /////// Two methods to load the configuration ////////
  
  protected static void loadWithCoreSettings() {
    // Use values in the server settings, first
    String sdir = Core.settings.getString("avs-" + settingsDirectory.name, settingsDirectory.getString());
    String cfile = Core.settings.getString("avs-" + configFile.name, configFile.getString());
    Fi file = Strings.getFiChild(subDir(sdir), cfile);
    logger.debugNormal("file: @", file.absolutePath());
    
    config = new DynamicSettings(file, true);
    config.setErrorHandler(logger::err);
    config.load();
  }
  
  /** Private variable to prevent a redirection loop */
  private static int configRedirectLimit = 5;
  
  /** 
   * Other method is to have a default file that redirect to the new. <br>
   * Instead of putting location in the server settings
   */
  protected static void loadWithFileRedirection() {
    Fi file = Strings.getFiChild(subDir(settingsDirectory.getString()), configFile.getString());
    logger.debugNormal("file: @", file.absolutePath());
    
    config = null;
    config = new DynamicSettings(file, true);
    config.setErrorHandler(logger::err);
    config.load();
    
    // If plugin or settings folders or config file name, are different from the default values, 
    // reload the configuration with values in these keys.
    if (!configFile.getString().equals(configFile.defaultValue) ||
        !pluginDirectory.getString().equals(pluginDirectory.defaultValue) ||
        !settingsDirectory.getString().equals(settingsDirectory.defaultValue)) {
      if (configRedirectLimit-- <= 0) throw new IllegalStateException("too many config file redirection");
      load();
      return;
    } else configRedirectLimit = 5; 
  }
  
  ///////////////
  
  
  public static class Field {
    public static final String bundleDescPrefix = "avs.config.";
    
    public final String name;
    public final Object defaultValue;
    public final boolean isDev, readOnly;
    protected final Func2<Object, Logger, Boolean> changed;
    
    Field(String name, Object defaultValue) {
      this(name, defaultValue, null, false, false);
    }
    
    Field(String name, Object defaultValue, boolean isDev) {
      this(name, defaultValue, null, isDev, false);
    }
    
    Field(String name, Object defaultValue, boolean isDev, boolean readOnly) {
      this(name, defaultValue, null, isDev, readOnly);
    }

    Field(String name, Object defaultValue, Func2<Object, Logger, Boolean> changed) {
      this(name, defaultValue, changed, false, false);
    }

    Field(String name, Object defaultValue, Func2<Object, Logger, Boolean> changed, boolean isDev) {
      this(name, defaultValue, changed, isDev, false);
    }

    Field(String name, Object defaultValue, Func2<Object, Logger, Boolean> changed, boolean isDev, boolean readOnly) {
      this.name = name;
      this.defaultValue = defaultValue;
      this.isDev = isDev;
      this.readOnly = readOnly;
      this.changed = changed == null ? (v, l) -> true : changed;

      all.add(this);
    }

    public boolean isInt(){
      return defaultValue instanceof Integer || defaultValue instanceof Long;
    }
    
    public boolean isFloat(){
      return defaultValue instanceof Float || defaultValue instanceof Double;
    }
    
    public boolean isBool(){
      return defaultValue instanceof Boolean;
    }
    
    public boolean isString(){
      return defaultValue instanceof String;
    }

    @SuppressWarnings("unchecked")
    public <T> T get() {
      // Allows getting values ​​if configuration is loading.
      if (isLoading) return (T) (config == null ? defaultValue : config.get(name, defaultValue));
      
      if (!isLoaded()) throw new IllegalStateException("config is not loaded. Use .loadConfig() first.");
      return (T) config.get(name, defaultValue);
    }
    
    public int getInt() {
      Object o = get();
      // Because simple json only store long and double.
      if (o instanceof Long) return (int) ((long) o);
      return (int) o;
    } 
    
    public float getFloat() {
      Object o = get();
      // Because simple json only store long and double.
      if (o instanceof Double) return (float) ((double) o);
      return (float) o;
    }
    
    public boolean getBool() {
      return get();
    }
    
    public String getString() {
      return get();
    }
    
    public boolean set(Object value) { return set(value, logger); }
    /** Return whether the change has been accepted or not */
    public boolean set(Object value, Logger logger) {
      if (!isLoaded()) throw new IllegalStateException("config is not loaded. Use .loadConfig() first.");
      if (readOnly) return false;
      
      Object old = get();
      config.put(name, value);
      boolean accept = changed.get(value, logger);
      if (!accept) { // restore to old
        if (old == defaultValue) config.remove(name);
        else config.put(name, old);
      }
      return accept;
    }
    
    public String getDescription() { return getDescription(logger); }
    /** Get description in the bundle using the name */
    public String getDescription(Logger logger) {
      return logger.getKey(bundleDescPrefix + name);
    }
    
    /** Only check the field name for equality */
    @Override
    public boolean equals(Object o) {
      if (o == this) return true;
      if (o == null || !(o instanceof Field)) return false;
      return name.equals(((Field) o).name);
    }
    
    @Override
    public String toString() {
      String v = "<unknown>";
      try { v = Strings.objToStr(get()); }
      catch (IllegalStateException ignored) {}
      return "{name=" + name + ", default=" + defaultValue + ", value=" + v + "}";
    }
  }
}
