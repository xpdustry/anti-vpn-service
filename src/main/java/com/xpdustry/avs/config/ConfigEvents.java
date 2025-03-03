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

import com.xpdustry.avs.Loader;
import com.xpdustry.avs.command.Command;
import com.xpdustry.avs.misc.CloudAutoRefresher;
import com.xpdustry.avs.misc.SettingsAutosave;
import com.xpdustry.avs.service.ServiceManager;
import com.xpdustry.avs.service.providers.ProviderAction;
import com.xpdustry.avs.service.providers.type.AddressProvider;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.bundle.L10NBundle;
import com.xpdustry.avs.util.logging.Logger;
import com.xpdustry.avs.util.network.AdvancedHttp;
import com.xpdustry.avs.util.network.AwaitHttp;

import arc.struct.ObjectMap;
import arc.struct.Seq;


/** Used by {@link AVSConfig} and {@link RestrictedModeConfig} to notify a changed value */
public class ConfigEvents {
  ////AVSConfig part
  
  static boolean onDefaultLocaleChanged(Object v, Logger logger) {
    if (!Loader.done()) return false;
      
    if (L10NBundle.bundles.find(b -> Strings.locale2String(b.locale).equalsIgnoreCase((String) v)) == null) {
      logger.err("avs.bundle.availables", v, 
                 Strings.listToSentence(logger, L10NBundle.bundles, b -> Strings.locale2String(b.locale)));
      return false;
    } 
    
    return Loader.loadBundles();
  }
  
  static boolean onConnectLimitChanged(Object v, Logger l) {
    return ServiceManager.setPoolSize();
  }
  
  static boolean onAutosaveSpacingChanged(Object v, Logger logger) {
    int s = (int) v;
    SettingsAutosave.spacing(Math.max(s, 1));
    
    if (!Loader.done()) return true;
    
    if (s == 0) SettingsAutosave.stop();
    else SettingsAutosave.start();
    
    return true;
  }
 
  static boolean onCleanupRecentsChanged(Object v, Logger logger) {
    ((com.xpdustry.avs.service.providers.custom.RecentRequestedCache) 
        com.xpdustry.avs.service.providers.type.OnlineServiceProvider.cacheProvider).scheduleCleanup(((int) v));
    return true;
  }
  
  static boolean onCloudRefreshTimeoutChanged(Object v, Logger logger) {
    int s = (int) v;
    CloudAutoRefresher.spacing(Math.max(s, 1));
    
    if (!Loader.done()) return true;
    
    if (s == 0) CloudAutoRefresher.stop();
    else CloudAutoRefresher.start();
    
    return true;
  }
  
  // dev settings callbacks
  static boolean onForceDebugChanged(Object v, Logger l) {
    Logger.forceDebug = (boolean) v;
    return true;
  }
  
  private static boolean validatePath(Object v, Logger logger) {
    boolean valid = Strings.isValidPath((String) v);
    if (!valid) logger.err("avs.command.config.err-invalid-path", v);
    return valid;
  }
  
  static boolean onPluginDirectoryChanged(Object v, Logger l) {
    return ((String) v).isEmpty() || validatePath(v, l);
  }
  
  static boolean onBundlesDirectoryChanged(Object v, Logger l) {
    return validatePath(v, l);
  }
  
  static boolean onCacheDirectoryChanged(Object v, Logger l) {
    return validatePath(v, l);
  }
  
  static boolean onSettingsDirectoryChanged(Object v, Logger l) {
    return validatePath(v, l);
  }

  static boolean onProviderDirectoryChanged(Object v, Logger l) {
    return validatePath(v, l);
  }
  
  static boolean onConfigFileChanged(Object v, Logger l) {
    return validatePath(v, l);
  }
  
  static boolean onAllowUntrustedSourceChanged(Object v, Logger l) {
    AdvancedHttp.allowUntrustedSourceHttpCode = (boolean) v;
    return true;
  }
  
  static boolean onSocketTimeoutChanged(Object v, Logger l) {
    AwaitHttp.readWriteTimeout = (int) v;
    return true;
  }
  
  static boolean onUseDefaultBundleChanged(Object v, Logger l) {
    L10NBundle.useDefaultWhenKeyNotFound = (boolean) v;
    return true;
  }
  
  static boolean onBundleCacheChanged(Object v, Logger l) {
    L10NBundle.useCache = (boolean) v;
    return true;
  }
  
  ////
  
  //// RestrictedModeConfig part
  
  static boolean onSettingsChanged(Seq<AVSConfig.ConfigField> settings, Logger logger) {
    for (AVSConfig.ConfigField c : settings) {
      if (c == null) {
        logger.err("avs.restrict.settings.invalid");
        return false;
      }
    }
    return true;
  }
  
  static boolean onProvidersChanged(Seq<AddressProvider> providers, Logger logger) {
    for (AddressProvider c : providers) {
      if (c == null) {
        logger.err("avs.restrict.providers.invalid");
        return false;
      }
    }
    return true;
  }
  
  static boolean onCommandsChanged(Seq<Command> commands, Logger logger) {
    for (Command c : commands) {
      if (c == null) {
        logger.err("avs.restrict.commands.invalid");
        return false;
      } else if (c.consoleOnly) {
        logger.err("avs.restrict.commands.console-only", c.name);
        return false;
      }
      
      // Some commands are critical and should not be allowed in restricted mode
      if (c instanceof com.xpdustry.avs.command.list.ResetCommand ||
          c instanceof com.xpdustry.avs.command.list.RestrictCommand)
        logger.warn("avs.restrict.commands.critial", c.name);
    }
    
    return true;
  }
  
  static boolean onActionsChanged(ObjectMap<AddressProvider, Seq<ProviderAction>> actions, Logger logger) {
    for (ObjectMap.Entry<AddressProvider, Seq<ProviderAction>> e : actions) {
      Seq<ProviderAction> valid = ProviderAction.getAll(e.key);
      Seq<ProviderAction> a = new Seq<>();
      
      for (int i=0; i<e.value.size; i++) {
        ProviderAction pa = e.value.get(i);
        
        if (pa == null) {
          logger.err("avs.restrict.actions.invalid", e.key.name);
          return false;
        }
        
        if (!valid.contains(pa)) a.add(pa);
      }
      
      if (!a.isEmpty()) {
        if (a.size == 1) logger.err("avs.restrict.actions.not-compatible", a.get(0).name, e.key.name);
        else logger.err("avs.restrict.actions.not-compatibles", Strings.listToSentence(logger, a, pa -> pa.name), 
                        e.key.name);
        return false;
      }
    }
    
    return true;
  }

  ////
}
