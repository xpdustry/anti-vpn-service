package avs;

import mindustry.Vars;
import avs.service.AntiVpnService;
import avs.service.ServiceManager;
import avs.util.DynamicSettings;
import avs.util.PVars;


public class Main extends mindustry.mod.Plugin {
  public void loadContent() {
    // TODO: welcome message
    PVars.setPluginFolder(Vars.modDirectory.child(Vars.mods.getMod(getClass()).name));
    DynamicSettings.logFile = PVars.settingsFolder.child("avs-logs.log");
    DynamicSettings.autosaveTimeout = mindustry.net.Administration.Config.autosaveSpacing.num();
    
    PVars.loadSettings();
    AntiVpnService.loadProviders();
  }

  public void init() {
    ServiceManager.registerServerListeners();
    
    // TODO: loading finished message
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
