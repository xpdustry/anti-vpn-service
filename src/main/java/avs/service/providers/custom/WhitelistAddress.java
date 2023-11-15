package avs.service.providers.custom;

import avs.service.providers.AddressValidity;


public class WhitelistAddress extends avs.service.providers.types.CustomAddressProvider {
  public WhitelistAddress() {
    super("Whitelist", "whitelist");
    customFolder = avs.util.PVars.settingsFolder; // Must be saved in plugin settings
  }

  @Override
  public boolean blockAddress(AddressValidity address) {
    boolean removed = cache.remove(address);
    if (removed) save();
    return removed;
  }

  @Override
  public boolean allowAddress(AddressValidity address) {
    boolean added = cache.addUnique(address);
    if (added) save();
    return added;
  }
}
