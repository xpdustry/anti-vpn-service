package avs;

import mindustry.Vars;

import avs.service.AntiVpnService;
import avs.service.ServiceManager;
import avs.util.PVars;


public class Main extends mindustry.mod.Plugin {
  public void loadContent() {
    PVars.setPluginFolder(Vars.modDirectory.child(Vars.mods.getMod(getClass()).name));
    PVars.loadSettings();
    AntiVpnService.loadSettins();
    AntiVpnService.loadProviders();
  }

  public void init() {
    ServiceManager.registerServerListener();
  }

  // Commands only for server console
  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    handler.register("anti-vpn", "[help|arg0] [arg1...]", "", args -> {

    });
    
    handler.register("avs-config", "[help|arg0] [arg1...]", "Configure many settings of Anti VPN Service", args -> {

    });
  }

}
