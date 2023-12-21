package avs;

import mindustry.Vars;

import arc.util.Log;
import avs.config.PVars;
import avs.service.AntiVpnService;
import avs.service.ServiceManager;
import avs.util.DynamicSettings;
import avs.util.network.AwaitHttp;


public class Main extends mindustry.mod.Plugin {
  public void init() {
    long start = System.currentTimeMillis();
    Log.info("----------------------------------------------------------------");
    Log.info("Anti Vpn Service (AVS) is loading...");
    Log.info("");
    // TODO: better welcome message
    
    PVars.setPluginFolder(Vars.modDirectory.child(Vars.mods.getMod(getClass()).name));
    DynamicSettings.logFile = PVars.settingsFolder.child("avs-logs.log");
    DynamicSettings.autosaveTimeout = mindustry.net.Administration.Config.autosaveSpacing.num();
    
    Log.info("Checking for updates...");
    // TODO: check for new version
    
    if (ServiceManager.registerServerListeners()) {
      PVars.loadSettings();
      AntiVpnService.loadProviders(); 
      AntiVpnService.logger.debug("Forcing autosave...");
      DynamicSettings.forceGlobalAutosave();
    }
    
    // TODO: loading finished message
    Log.info("");
    Log.info("Loading finished in @ seconds", ((float) (System.currentTimeMillis()-start)/1000));
    Log.info("----------------------------------------------------------------");
  }

  // Commands only for server console
  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    handler.register("anti-vpn", "[help|arg0] [arg1...]", "Configure providers settings of Anti VPN Service", args -> {

    });
    
    handler.register("avs-config", "[help|arg0] [arg1...]", "Configure main settings of Anti VPN Service", args -> {

    });
  }

}
