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

package com.xpdustry.avs.config;

import arc.Core;
import arc.files.Fi;
import arc.struct.Seq;

import com.xpdustry.avs.util.DynamicSettings;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.Strings;


public class AVSConfig {
  public static final Seq<ConfigField> all = new Seq<>();
  protected static Fi workingDirectory;
  protected static DynamicSettings config;  
  protected static boolean isLoading = false;
  protected static Logger logger = new Logger("Config");
  
  public static final ConfigField
    enabled = new ConfigField("enabled", true),
    defaultLocale = new ConfigField("default-locale", "en", ConfigEvents::onDefaultLocaleChanged),
    kickMessage = new ConfigField("kick-message", ""),
    serverBusyMessage = new ConfigField("busy-message", ""),
    errorMessage = new ConfigField("error-message", ""),
    clientKickDuration = new ConfigField("kick-duration", 30),
    connectLimit = new ConfigField("connect-limit", arc.util.OS.cores, ConfigEvents::onConnectLimitChanged),
    tokenCheckTimeout = new ConfigField("token-timeout", 50),
    serviceCheckTimeout = new ConfigField("service-timeout", 30),
    startupDownload = new ConfigField("startup-download", true),
    autosaveSpacing = new ConfigField("autosave-spacing", 60 * 15, ConfigEvents::onAutosaveSpacingChanged),
    resetCommandEnabled = new ConfigField("reset-command", false, false, true),
    restrictedSettings = new ConfigField("restricted-settings", Strings.join(",", connectLimit.name, kickMessage.name, serverBusyMessage.name, errorMessage.name), ConfigEvents::onRestrictedSettingsChanged),
    restrictedProviders = new ConfigField("restricted-providers", String.join(",", "whitelist", "blacklist"), ConfigEvents::onRestrictedProvidersChanged),
    restrictedCommands = new ConfigField("restricted-commands", String.join(",", "config", "provider", "info", "help"), ConfigEvents::onRestrictedCommandsChanged),
    useDefaultBundle = new ConfigField("bundle-default", com.xpdustry.avs.util.bundle.L10NBundle.useDefaultWhenKeyNotFound, ConfigEvents::onUseDefaultBundleChanged),
    randomOnlineProviders = new ConfigField("random-online", false),
    randomTokens = new ConfigField("random-tokens", false),
    preventUnavailable = new ConfigField("prevent-unavailable", true),
    resultRequired = new ConfigField("result-required", true),
    cleanupRecents = new ConfigField("cleanup-recents", false),
    cloudRefreshTimeout = new ConfigField("cloud-refresh", 720),
    
    // For devs, better to keep that as default
    pluginDirectory = new ConfigField("plugin-dir", "", ConfigEvents::onPluginDirectoryChanged, true),
    bundlesDirectory = new ConfigField("bundles-dir", "bundles", ConfigEvents::onBundlesDirectoryChanged, true),
    cacheDirectory = new ConfigField("cache-dir", "cache", ConfigEvents::onCacheDirectoryChanged, true),
    cloudDirectory = new ConfigField("cloud-dir", cacheDirectory.defaultValue + "/cloud", ConfigEvents::onCloudDirectoryChanged, true),
    settingsDirectory = new ConfigField("settings-dir", "settings", ConfigEvents::onSettingsDirectoryChanged, true),
    tokensDirectory = new ConfigField("tokens-dir", settingsDirectory.defaultValue + "/tokens", ConfigEvents::onTokensDirectoryChanged, true), 
    configFile = new ConfigField("config-file", "config.json", ConfigEvents::onConfigFileChanged, true),
    defaultCacheFileExt = new ConfigField("cache-ext", "bin", true),
    allowUntrustedSource = new ConfigField("allow-http203", com.xpdustry.avs.util.network.AdvancedHttp.allowUntrustedSourceHttpCode, ConfigEvents::onAllowUntrustedSourceChanged, true),
    socketTimeout = new ConfigField("socket-timeout", com.xpdustry.avs.util.network.AwaitHttp.readWriteTimeout, ConfigEvents::onSocketTimeoutChanged, true),
    useBundleCache = new ConfigField("bundle-cache", com.xpdustry.avs.util.bundle.L10NBundle.useCache, ConfigEvents::onBundleCacheChanged, true),
    none = null
    ;

  
  public static ConfigField get(String name) {
    return all.find(f -> f.name.equals(name));
  }
  
  protected static void setAllToDefault() {
    all.each(s -> config.put(s.name, s.defaultValue));
  }
  
  public static void notifyAllValueChanged() {
    all.each(s -> s.changed.run(s.get(), logger));
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
}
