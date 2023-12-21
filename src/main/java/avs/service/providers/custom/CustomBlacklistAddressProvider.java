package avs.service.providers.custom;


public class CustomBlacklistAddressProvider extends avs.service.providers.types.CustomAddressProvider {
  public CustomBlacklistAddressProvider() {
    super("Custom Blacklist", "custom-blacklist");
    customFolder = avs.config.PVars.settingsFolder; // for custom blacklist, it's stored in plugin settings
  }
}
