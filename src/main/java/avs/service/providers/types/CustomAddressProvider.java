package avs.service.providers.types;

import avs.service.providers.IPValidity;

public abstract class CustomAddressProvider extends AddressProvider {
  public CustomAddressProvider(String name) { super(name); }
  public CustomAddressProvider(String displayName, String name) { super(displayName, name); }

  public abstract void reloadSettings();
  public abstract void saveSettings();
  public abstract void blockAddress(IPValidity address);
  public abstract boolean allowAddress(IPValidity address);
}
