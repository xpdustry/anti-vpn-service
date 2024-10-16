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

package com.xpdustry.avs;

import com.xpdustry.avs.misc.AVSConfig;
import com.xpdustry.avs.misc.AVSEvents;
import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.ServiceManager;
import com.xpdustry.avs.util.DynamicSettings;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.VersionChecker;
import com.xpdustry.avs.util.bundle.L10NBundle;

import arc.Events;
import arc.files.Fi;

import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;;


public class Loader {
  private static final Logger logger = new Logger();
  private static boolean loaded = false;
  private static LoadedMod mod;
  
  public static void load(Class<? extends mindustry.mod.Mod> modClass) {
    if (loaded) throw new IllegalStateException("already loaded");
    
    Events.fire(new AVSEvents.AVSLoadingEvent());
    
    mod = Vars.mods.getMod(modClass);
    if (mod == null) throw new IllegalArgumentException("the specified mod is not loaded");
    
    loadSettings(Vars.modDirectory.child(mod.meta.name)); // First, load settings
    loadBundles(); // After, the bundles
    finishSettingsLoading(); // Complete the settings loading
    VersionChecker.checkAndPromptToUpgrade(mod.meta.repo, mod.meta.version); // Check the version
    initPlugin(); // And init the plugin
    
    loaded = true;
    
    Events.fire(new AVSEvents.AVSLoadedEvent());
  }
  
  public static boolean done() {
    return loaded;
  }
  
  public static void loadSettings(Fi workingDirectory) {
    AVSConfig.setWorkingDirectory(workingDirectory);
    AVSConfig.load();
    DynamicSettings.logFile = AVSConfig.subDir(AVSConfig.settingsDirectory.getString())
                                       .child(workingDirectory.name() + ".log");
    DynamicSettings.autosaveSpacing = AVSConfig.autosaveSpacing.getInt();
  }
  
  /** Because we need to load the bundles before trigger the initial value change listener */
  public static void finishSettingsLoading() {
    AVSConfig.notifyAllValueChanged();
  }
  
  public static void initPlugin() {
    if (ServiceManager.registerListeners()) {
      AntiVpnService.load(); 
      DynamicSettings.globalAutosave();
    }
  }
  
  public static void loadBundles() {
    L10NBundle.load(mod.root.child("bundles"), AVSConfig.defaultLocale.getString());
    logger.debug("avs.loading.custom-bundles");
    Fi bundles = AVSConfig.subDir(AVSConfig.bundlesDirectory.getString());
    bundles.mkdirs();
    L10NBundle.appendBundles(bundles);
    logger.info("avs.loading.bundle-loaded", L10NBundle.bundles.size, L10NBundle.defaultLocale);
  }
}
