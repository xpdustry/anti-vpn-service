/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024-2025 Xpdustry
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

package com.xpdustry.avs.config;

import arc.files.Fi;

import com.xpdustry.avs.util.config.*;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.logging.Logger;


public class AVSConfig extends AConfig {
  private static final AVSConfig INSTANCE = new AVSConfig();
  private static Fi workingDirectory;
  /** Limit to prevent a config redirection loop */
  private static int configRedirectLimit = 5;
  
  private AVSConfig() { super("config", true); }
  public static AVSConfig instance() { return INSTANCE; }

  public static Fi subDir(String path) {
    // use the custom plugin directory first
    if (INSTANCE.config != null || INSTANCE.isLoading) {
      String pd =  pluginDirectory.getString();
      if (!pd.isBlank()) 
        setWorkingDirectory(Strings.getFiChild(arc.Core.settings.getDataDirectory(), pd));
    }
    // By default, use the mods directory
    if (workingDirectory == null) setWorkingDirectory(mindustry.Vars.modDirectory);
    
    return Strings.getFiChild(workingDirectory, path);
  }
  
  public static Fi subDir(Fi path) {
    return subDir(path.path());
  }
  
  public static void setWorkingDirectory(Fi wd) {
    if (INSTANCE.isLoaded()) 
      throw new IllegalStateException("cannot change working directory after loaded config");
    workingDirectory = wd;
  }
  
  public static Fi getWorkingDirectory() {
    return workingDirectory;
  }

  @Override
  protected String getFieldDescKey(Field<?> field) {
    return "avs." + name + '.' + field.name;
  }
  
  @Override
  public void loadMisc() {
    // If plugin or settings folders or config file name, are different from the default values, 
    // reload the configuration with values in these keys.
    if (!configFile.getString().equals(configFile.defaultValue()) ||
        !pluginDirectory.getString().equals(pluginDirectory.defaultValue()) ||
        !settingsDirectory.getString().equals(settingsDirectory.defaultValue())) {
      if (configRedirectLimit-- <= 0) {
        config = null;
        throw new IllegalStateException("too many config file redirection");
      }
      load();
      
    } else configRedirectLimit = 5;    
  }
  
  @Override
  protected Fi getFile() {
    return Strings.getFiChild(subDir(settingsDirectory.getString()), configFile.getString());
  }
  
  
  public static class ConfigField extends SimpleField {
    public final boolean isDev, readOnly;
    
    ConfigField(String name, Object defaultValue) { this(name, defaultValue, null, false, false); }
    ConfigField(String name, Object defaultValue, boolean isDev) { this(name, defaultValue, null, isDev, false); }
    ConfigField(String name, Object defaultValue, boolean isDev, boolean readOnly) { this(name, defaultValue, null, isDev, readOnly); }
    ConfigField(String name, Object defaultValue, ChangeValider<Object> validate) { this(name, defaultValue, validate, false, false); }
    ConfigField(String name, Object defaultValue, ChangeValider<Object> validate, boolean isDev) { this(name, defaultValue, validate, isDev, false); }
    ConfigField(String name, Object defaultValue, ChangeValider<Object> validate, boolean isDev, boolean readOnly) {
      super(INSTANCE, name, defaultValue, validate);
      this.isDev = isDev;
      this.readOnly = readOnly;
    }

    @Override
    public void load() {
      value = master.config().get(name, type, elementType, keyType, defaultValue);
      modified = false;
    }
    
    /** Handle {@link #readOnly} */
    @Override
    public boolean set(Object value, Logger logger) { 
      return !readOnly && super.set(value, logger); 
    }
    
    @Override
    public void forcesave() {
      if (value == defaultValue) master.config().remove(name);
      else super.forcesave();
    }
  }

  
  /** Config variables */
  public static final ConfigField
    enabled = new ConfigField("enabled", true),
    defaultLocale = new ConfigField("default-locale", "en", ConfigEvents::onDefaultLocaleChanged),
    kickMessage = new ConfigField("kick-message", ""),
    serverBusyMessage = new ConfigField("busy-message", ""),
    errorMessage = new ConfigField("error-message", ""),
    clientKickDuration = new ConfigField("kick-duration", 30),
    connectLimit = new ConfigField("connect-limit", arc.util.OS.cores, ConfigEvents::onConnectLimitChanged),
    startupDownload = new ConfigField("startup-download", true),
    autosaveSpacing = new ConfigField("autosave-spacing", 60 * 15, ConfigEvents::onAutosaveSpacingChanged),
    resetCommandEnabled = new ConfigField("reset-command", false, false, true),
    useDefaultBundle = new ConfigField("bundle-fallback", com.xpdustry.avs.util.bundle.L10NBundle.useDefaultWhenKeyNotFound, ConfigEvents::onUseDefaultBundleChanged),
    randomOnlineProviders = new ConfigField("random-online", false),
    randomTokens = new ConfigField("random-tokens", false),
    preventUnavailable = new ConfigField("prevent-unavailable", true),
    resultRequired = new ConfigField("result-required", true),
    cleanupRecents = new ConfigField("cleanup-recents", 60, ConfigEvents::onCleanupRecentsChanged),
    cloudRefreshTimeout = new ConfigField("cloud-refresh", 360, ConfigEvents::onCloudRefreshTimeoutChanged),
    
    // For devs, better to keep that as default
    forceDebug = new ConfigField("force-debug", false, ConfigEvents::onForceDebugChanged, true),
    pluginDirectory = new ConfigField("plugin-dir", "", ConfigEvents::onPluginDirectoryChanged, true),
    bundlesDirectory = new ConfigField("bundles-dir", "bundles", ConfigEvents::onBundlesDirectoryChanged, true),
    cacheDirectory = new ConfigField("cache-dir", "cache", ConfigEvents::onCacheDirectoryChanged, true),
    settingsDirectory = new ConfigField("settings-dir", "settings", ConfigEvents::onSettingsDirectoryChanged, true),
    providersDirectory = new ConfigField("providers-dir", settingsDirectory.defaultValue() + "/providers", ConfigEvents::onProviderDirectoryChanged, true),
    configFile = new ConfigField("config-file", INSTANCE.name + ".json", ConfigEvents::onConfigFileChanged, true),
    allowUntrustedSource = new ConfigField("allow-http203", com.xpdustry.avs.util.network.AdvancedHttp.allowUntrustedSourceHttpCode, ConfigEvents::onAllowUntrustedSourceChanged, true),
    socketTimeout = new ConfigField("socket-timeout", com.xpdustry.avs.util.network.AwaitHttp.readWriteTimeout, ConfigEvents::onSocketTimeoutChanged, true),
    useBundleCache = new ConfigField("bundle-cache", com.xpdustry.avs.util.bundle.L10NBundle.useCache, ConfigEvents::onBundleCacheChanged, true),
    none = null;
}
