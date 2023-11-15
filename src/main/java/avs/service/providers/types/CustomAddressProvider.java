package avs.service.providers.types;

import avs.service.providers.AddressValidity;
import avs.util.Logger;

public abstract class CustomAddressProvider extends AddressProvider {
  public CustomAddressProvider(String name) { super(name); }
  public CustomAddressProvider(String displayName, String name) { super(displayName, name); }

  
  @Override
  public void load() {
    super.load();
    
    if (cache.isEmpty()) Logger.err("Failed to load addresses from provider '@'! Skipping it...", displayName);
    else Logger.info("Loaded @ addresses for provider '@'.", cache.size, displayName);
  }
  
  public boolean blockAddress(AddressValidity address) {
    // TODO: fire an event. Wanted?
    boolean added = cache.addUnique(address);
    if (added) save();
    return added;
  }

  public boolean allowAddress(AddressValidity address) {
    // TODO: fire an event. Wanted?
    boolean removed = cache.remove(address);
    if (removed) save();
    return removed;
  }
}
