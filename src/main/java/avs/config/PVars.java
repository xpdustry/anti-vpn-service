/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2023 Xpdustry
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

package avs.config;

import arc.files.Fi;
import arc.struct.ObjectMap;

import avs.util.DynamicSettings;
import avs.util.Logger;
import avs.util.Strings;


public class PVars {
  public static ObjectMap<String, Integer> potentialDDoSIPs = new ObjectMap<>();
  public static Fi pluginFolder, cacheFolder, settingsFolder, tokensFolder;
  public static String whitelistProviderName = "whitelist", 
      flaggedCacheProviderName = "flagged-address",
      kickMessage = "Your ip is flagged as VPN!",
      ipFormat = "\n\n[lightgray]IP: @[]",
      serverBusyMessage = "Server busy! \n[lightgray]Please try again later.[]";
  public static int threadPoolSize = arc.util.OS.cores,
      // Timeout to re-check the validity of token when one has reached it's limit
      tokenValdityCheckTimeout = 100, // re-check the validity after X player ip check
      serviceValidityTimeout = 30,
      clientRecentKickDuration = 30 * 1000;
  public static boolean serviceEnabled = true, printIP = true;
  
  private static DynamicSettings settingsFile = null;
  
  static {
    // To have a default folder
    setPluginFolder(mindustry.Vars.modDirectory);
  }
  
  public static void setPluginFolder(Fi folder) {
    pluginFolder = folder;
    cacheFolder = folder.child("cache");
    settingsFolder = folder.child("settings");
    tokensFolder = folder.child("tokens");
  }
  
  public static void loadSettings() {
    // TODO: save this to plain text json instead
    
    if (getSettingsFile().has("avs-settings")) {
      boolean[] settings = Strings.integer2binary(getSettingsFile().getLong("avs-settings"), 2);

      // Avoid errors when adding new settings
      try {
        serviceEnabled = settings[1];
        printIP = settings[2];
      } catch (IndexOutOfBoundsException e) {
        saveSettings();
      }
    }
  }

  public static void saveSettings() {
    // TODO: put settings for providers
    
    getSettingsFile().put("avs-settings", 
        Strings.binary2integer(serviceEnabled, printIP));
  }
  
  public static DynamicSettings getSettingsFile() {
    if (settingsFile == null) {
      settingsFile = new DynamicSettings(settingsFolder.child("settings.bin"));
      settingsFile.setErrorHandler(new Logger()::err);
      settingsFile.load();
    }
    
    return settingsFile;
  }
}
