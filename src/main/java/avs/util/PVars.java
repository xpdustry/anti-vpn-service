package avs.util;

import arc.Core;
import arc.files.Fi;
import mindustry.Vars;


public class PVars {
  public static Fi pluginFolder, cacheFolder, settingsFolder, tokensFolder;
  public static String whitelistProviderName = "whitelist", flaggedCacheProviderName = "flagged-address";
  public static int threadPoolSize = arc.util.OS.cores * 2,
      // Timeout to re-check the validity of token when one has reached it's limit
      tokenValdityCheckTimeout = 100, // re-check the validity after X player ip check
      serviceValidityTimeout = 30;
  public static boolean serviceEnabled = true, printIP = true;
  
  private static DynamicSettings settingsFile = null;
  
  static {
    // To have a default folder
    setPluginFolder(Vars.modDirectory);
  }
  
  public static void setPluginFolder(Fi folder) {
    pluginFolder = folder;
    cacheFolder = folder.child("cache");
    settingsFolder = folder.child("settings");
    tokensFolder = folder.child("tokens");
  }
  
  public static void loadSettings() {
    // TODO: save this to plain text json
    if (settingsFile == null) {
      settingsFile = new DynamicSettings(settingsFolder.child("settings.bin"));
      settingsFile.load();
    }
    
    if (settingsFile.has("avs-settings")) {
      boolean[] settings = Strings.integer2binary(Core.settings.getInt("avs-settings"), 2);

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
    if (settingsFile == null) {
      settingsFile = new DynamicSettings(settingsFolder.child("settings.bin"));
      settingsFile.load();
    }
    
    // TODO: put settings for providers
    
    settingsFile.put("avs-settings", 
        Strings.binary2integer(serviceEnabled, printIP));
  }
}
