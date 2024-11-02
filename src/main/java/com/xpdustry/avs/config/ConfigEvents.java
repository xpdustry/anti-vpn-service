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

import com.xpdustry.avs.Loader;
import com.xpdustry.avs.command.AVSCommandManager;
import com.xpdustry.avs.command.Command;
import com.xpdustry.avs.command.list.ConfigCommand;
import com.xpdustry.avs.command.list.ProviderCommand;
import com.xpdustry.avs.misc.CloudAutoRefresher;
import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.ServiceManager;
import com.xpdustry.avs.service.providers.type.AddressProvider;
import com.xpdustry.avs.util.DynamicSettings;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.bundle.L10NBundle;
import com.xpdustry.avs.util.network.AdvancedHttp;
import com.xpdustry.avs.util.network.AwaitHttp;

import arc.Core;
import arc.struct.Seq;


/** Used by {@link AVSConfig} to notify a changed value */
public class ConfigEvents {
  public static interface Callback {
    boolean run(Object value, Logger logger);
  }

  public static boolean onDefaultLocaleChanged(Object v, Logger logger) {
    if (!Loader.done()) return false;
      
    if (L10NBundle.bundles.find(b -> Strings.locale2String(b.locale).equalsIgnoreCase((String) v)) == null) {
      String list;
      if (L10NBundle.defaultBundle.locale.getLanguage().equals("en"))
           list = Strings.listToSentence(L10NBundle.bundles, b -> Strings.locale2String(b.locale));
      else list = L10NBundle.bundles.toString(", ", b -> Strings.locale2String(b.locale));
      
      logger.err("avs.bundle.availables", v, list);
      return false;
    }
    
    Loader.loadBundles();
    return true;
  }
  
  public static boolean onConnectLimitChanged(Object v, Logger l) {
    return ServiceManager.setPoolSize();
  }
  
  public static boolean onAutosaveSpacingChanged(Object v, Logger logger) {
    int i = (int) v;
    DynamicSettings.setAutosaveSpacing(Math.max(i, 1));
    
    if (!Loader.done()) return true;
    
    if (i == 0) DynamicSettings.stopAutosave();
    else DynamicSettings.startAutosave("AVS-Autosave");
    
    return true;
  }
  
  public static boolean onRestrictedSettingsChanged(Object v, Logger logger) {
    String value = (String) v;
    Seq<ConfigField> list = new Seq<>();
    
    if (value.equals("all")) {
      ConfigCommand.restrictedSettings.set(AVSConfig.all);
      logger.info("avs.command.config.allowed-all");
      return true;
    }
    
    if (!value.equals("\"\"") && !value.isBlank()) {
      for (String s : value.split(",")) {
        final String s0 = s.strip();
        ConfigField config = AVSConfig.all.find(c -> c.name.equals(s0));
        if (config == null) {
          logger.err("avs.command.config.field.not-found", s0);
          return false;
        }
        list.add(config);
      }      
    }
    
    ConfigCommand.restrictedSettings.set(list);
    return true;
  }
  
  public static boolean onRestrictedProvidersChanged(Object v, Logger logger) {
    String value = (String) v;
    Seq<AddressProvider> list = new Seq<>();
    
    if (value.equals("all")) {
      ProviderCommand.restrictedProviders.set(AntiVpnService.allProviders);
      logger.info("avs.command.provider.allowed-all");
      return true;
    }
    
    if (!value.equals("\"\"") && !value.isBlank()) {
      for (String p : value.split(",")) {
        final String p0 = p.strip();
        AddressProvider provider = AntiVpnService.allProviders.find(c -> c.name.equals(p0) || c.displayName.equals(p0));
        if (provider == null) {
          logger.err("avs.command.provider.not-found", p0);
          return false;
        }
        list.add(provider);
      }      
    }
    
    ProviderCommand.restrictedProviders.set(list);
    return true;
  }

  public static boolean onRestrictedCommandsChanged(Object v, Logger logger) {
    String value = (String) v;
    Seq<Command> list = new Seq<>();
    
    if (value.equals("all")) {
      AVSCommandManager.restrictedCommands.set(AVSCommandManager.subCommands);
      logger.info("avs.command.allowed-all");
      return true;
    }
    
    if (!value.equals("\"\"") && !value.isBlank()) {
      for (String c : value.split(",")) {
        final String c0 = c.strip();
        Command command = AVSCommandManager.subCommands.find(a -> a.name.equals(c0));
        if (command == null) {
          logger.err("avs.command.not-found2", c0);
          return false;
        }
        list.add(command);
      }
    }
    
    AVSCommandManager.restrictedCommands.set(list);
    return true;
  }
  
  public static boolean onCleanupRecentsChanged(Object v, Logger logger) {
    ((com.xpdustry.avs.service.providers.custom.RecentRequestedCache) 
        com.xpdustry.avs.service.providers.type.OnlineServiceProvider.cacheProvider).scheduleCleanup(((int) v));
    return true;
  }
  
  public static boolean onCloudRefreshTimeoutChanged(Object v, Logger logger) {
    int i = (int) v;
    CloudAutoRefresher.spacing(Math.max(i, 1));
    
    if (!Loader.done()) return true;
    
    if (i == 0) CloudAutoRefresher.stop();
    else CloudAutoRefresher.start();
    
    return true;
  }
  
  // dev settings callbacks
  
  private static boolean validatePath(Object v, Logger logger) {
    boolean valid = Strings.isValidPath((String) v);
    if (!valid) logger.err("avs.command.config.err-invalid-path", v);
    return valid;
  }
  
  public static boolean onPluginDirectoryChanged(Object v, Logger l) {
    String s = (String) v;
    Core.settings.put("avs-" + AVSConfig.pluginDirectory.name, s);
    return s.isEmpty() || validatePath(s, l);
  }
  
  public static boolean onBundlesDirectoryChanged(Object v, Logger l) {
    return validatePath(v, l);
  }
  
  public static boolean onCacheDirectoryChanged(Object v, Logger l) {
    return validatePath(v, l);
  }
  
  public static boolean onCloudDirectoryChanged(Object v, Logger l) {
    return validatePath(v, l);
  } 
  
  public static boolean onSettingsDirectoryChanged(Object v, Logger l) {
    Core.settings.put("avs-" + AVSConfig.settingsDirectory.name, (String) v);
    return validatePath(v, l);
  }

  public static boolean onProviderDirectoryChanged(Object v, Logger l) {
    return validatePath(v, l);
  }
  
  public static boolean onConfigFileChanged(Object v, Logger l) {
    Core.settings.put("avs-" + AVSConfig.configFile.name, (String) v);
    return validatePath(v, l);
  }
  
  public static boolean onAllowUntrustedSourceChanged(Object v, Logger l) {
    AdvancedHttp.allowUntrustedSourceHttpCode = (boolean) v;
    return true;
  }
  
  public static boolean onSocketTimeoutChanged(Object v, Logger l) {
    AwaitHttp.readWriteTimeout = (int) v;
    return true;
  }
  
  public static boolean onUseDefaultBundleChanged(Object v, Logger l) {
    L10NBundle.useDefaultWhenKeyNotFound = (boolean) v;
    return true;
  }
  
  public static boolean onBundleCacheChanged(Object v, Logger l) {
    L10NBundle.useCache = (boolean) v;
    return true;
  }
}
