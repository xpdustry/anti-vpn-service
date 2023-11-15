package avs.service.providers.custom;


public class UserAddressList extends avs.service.providers.types.CustomAddressProvider {
  public UserAddressList() {
    super("Custom Blacklist", "custom-blacklist");
    customFolder = avs.util.PVars.settingsFolder; // for custom blacklist, it's stored in plugin settings
  }
}
