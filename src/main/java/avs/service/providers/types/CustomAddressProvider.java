package avs.service.providers.types;

import avs.service.providers.AddressValidity;

public abstract class CustomAddressProvider extends AddressProvider {
  public CustomAddressProvider(String name) { super(name); }
  public CustomAddressProvider(String displayName, String name) { super(displayName, name); }

  public void blockAddress(AddressValidity address) {
    // TODO: fire an event. Wanted?
    if (cache.addUnique(address)) save();
  }

  public boolean allowAddress(AddressValidity address) {
    // TODO: fire an event. Wanted?
    boolean removed = cache.remove(address);
    if (removed) save();
    return removed;
  }
}
