package avs;

import mindustry.Vars;

import avs.service.AntiVpnService;
import avs.service.ServiceManager;
import avs.util.PVars;


public class Main extends mindustry.mod.Plugin {
  public void loadContent() {
    PVars.cacheFolder = Vars.modDirectory.child(Vars.mods.getMod(getClass()).name).child("cache");
    PVars.loadSettings();
    AntiVpnService.loadSettins();
    AntiVpnService.loadProviders();
  }

  public void init() {
    ServiceManager.registerServerListener();
  }

  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    // Command only for server console
    handler.register("anti-vpn", "[help|arg0] [arg1...]", "", args -> {

    });
  }

}
