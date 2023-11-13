package service.providers.types;


public abstract class CustomAddressProvider extends AddressProvider {
  public CustomAddressProvider(String name) { super(name); }
  public CustomAddressProvider(String displayName, String name) { super(displayName, name); }

  public abstract void reloadSettings();
  public abstract void saveSettings();
  public abstract void blockIP(String address);
  public abstract boolean allowIP(String address);
}
