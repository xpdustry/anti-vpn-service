package avs.service.providers.types;

import avs.service.providers.AddressValidity;


public abstract class CustomAddressProvider extends AddressProvider {
  public CustomAddressProvider(String name) { super(name); }
  public CustomAddressProvider(String displayName, String name) { super(displayName, name); }

  
  @Override
  public boolean load() {
    boolean loaded = super.load();
    
    if (!loaded) logger.err("Failed to load addresses! Skipping it...");
    else if (cache.isEmpty()) logger.warn("No addresses found.");
    else logger.info("Loaded @ addresses from cache file.", cache.size);
    
    return loaded;
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
