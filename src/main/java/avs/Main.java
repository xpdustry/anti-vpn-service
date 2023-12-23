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

package avs;

import arc.util.Log;

import mindustry.Vars;

import avs.config.PVars;
import avs.service.AntiVpnService;
import avs.service.ServiceManager;
import avs.util.DynamicSettings;
import avs.util.Strings;
import avs.util.network.AwaitHttp;


public class Main extends mindustry.mod.Plugin {
  public void init() {
    long start = System.currentTimeMillis();
    
    Log.info("");
    Log.info("&lg----------------------------------------------------------------");
    Log.info("Anti Vpn Service (AVS) is loading...");
    Log.info("");
    // TODO: better welcome message
    
    mindustry.mod.Mods.ModMeta meta = Vars.mods.getMod(getClass()).meta;
    checkForUpdates(meta.repo, meta.version);
    initPlugin(Vars.modDirectory.child(meta.name));
    
    Log.info("");
    Log.info("Loading finished in @ seconds", ((float) (System.currentTimeMillis()-start)/1000));
    Log.info("&lg----------------------------------------------------------------");
    Log.info("");
  }
  
  public void initPlugin(arc.files.Fi workingDirectory) {
    PVars.setPluginFolder(workingDirectory);
    DynamicSettings.logFile = PVars.settingsFolder.child(workingDirectory.name() + "_files.log");
    DynamicSettings.autosaveTimeout = mindustry.net.Administration.Config.autosaveSpacing.num();

    if (ServiceManager.registerServerListeners()) {
      PVars.loadSettings();
      AntiVpnService.loadProviders(); 
      AntiVpnService.logger.debug("Forcing autosave...");
      DynamicSettings.forceGlobalAutosave();
    }
  }

  public void checkForUpdates(String repo, String currentVersion) {
    Log.info("Checking for updates...");
    
    if (repo == null || repo.isBlank()) {
      Log.warn("No repo found for update");
      return;
    }
    if (currentVersion == null || currentVersion.isBlank()) {
      Log.warn("No current version found for update");
      return;
    }
    
    if (repo.endsWith("/")) repo = repo.substring(0, repo.lastIndexOf('/'));
    if (!repo.endsWith("/releases/latest")) repo = repo + "/releases/latest";
    final String repo0 = repo;

    AwaitHttp.get(repo, success -> {
      String result = success.getResultAsString();
      if (result.isBlank()) throw new Exception("Reply content is empty");
      arc.util.serialization.JsonValue json = new arc.util.serialization.JsonReader().parse(result), tag_name = json.get("tag_name");
      if (tag_name == null) throw new Exception("Unable to find 'tag_name'");
      
      // Extract the version
      String version = tag_name.asString();
      if (version.startsWith("v")) version = version.substring(1);
      int dot = version.indexOf('.');
      int major = Strings.parseInt(dot == -1 ? version : version.substring(0, dot), 0);
      int minor = dot == -1 ? 0 : Strings.parseInt(version.substring(dot + 1), 0);

      if (mindustry.core.Version.isAtLeast(major, minor, currentVersion)) {
        Log.info("New version found: @", version);
        Log.info("Check out this link to upgrade it: @", repo0);
      }
      
    }, failure -> Log.err("Unable to check for updates: " + failure.getLocalizedMessage()));
  }
  
  // Commands only for console
  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    handler.register("anti-vpn", "[help|arg0] [arg1...]", "Configure providers settings of Anti VPN Service", args -> {

    });
    
    handler.register("avs-config", "[help|arg0] [arg1...]", "Configure main settings of Anti VPN Service", args -> {

    });
  }

}
