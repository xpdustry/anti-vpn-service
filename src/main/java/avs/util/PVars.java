package avs.util;

import arc.Core;
import arc.files.Fi;
import mindustry.Vars;


public class PVars {
  public static Fi pluginFolder, cacheFolder, settingsFolder, tokensFolder;
  public static int threadPoolSize = 100;
  public static boolean serviceEnabled = true, printIP = true;
  
  
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
    if (Core.settings.has("avs-settings")) {
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
    Core.settings.put("avs-settings", 
        Strings.binary2integer(serviceEnabled, printIP));
  }
}
