package avs.util;

import arc.Core;

public class PVars {
  public static arc.files.Fi cacheFolder = mindustry.Vars.modDirectory.child("cache");
  public static int threadPoolSize = 100;
  public static boolean serviceEnabled = true,
      printIP = true;
  
  
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
