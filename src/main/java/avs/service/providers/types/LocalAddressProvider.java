package avs.service.providers.types;

import arc.struct.Seq;

import avs.service.providers.AddressValidity;
import avs.util.Logger;
import avs.util.PVars;
import avs.util.Subnet;


public abstract class LocalAddressProvider extends AddressProvider {
  /* Define the type of provider, used for statistics. Default is VPN */
  protected LocalAddressProviderType providerType = LocalAddressProviderType.basic;
  
  public LocalAddressProvider(String displayName, String name) { 
    super(displayName, name); 
    customFolder = PVars.cacheFolder.child("cloud");
  }
  public LocalAddressProvider(String name) { 
    super(name); 
    customFolder = PVars.cacheFolder.child("cloud");
  }

  @Override
  public void load() {
    Seq<Subnet> list = downloadList();
    
    if (list == null) throw new NullPointerException(".downloadList() should not return null");
    list.removeAll(v -> v == null);
    
    // If failed to fetch list, use cached list
    if (list.isEmpty()) {
      Logger.warn("Failed to fetch addresses from cloud provider '@'! Using cached list...", displayName);
      reload();
      
    } else {
      Logger.info("Found @ addresses for cloud provider '@'. Saving it in cache...", list.size, displayName);
      cache.clear();
      list.each(s -> {
        AddressValidity valid = new AddressValidity(s);
        valid.isVPN = providerType.isVPN();
        valid.isProxy = providerType.isProxy();
        valid.isTOR = providerType.isTOR();
        valid.isRelay = providerType.isRelay();
        valid.isOther = providerType.isOther();
        
        cache.add(valid);
      });
      save();
    }
  }
  
  // Reload only cache. To reload the all, call .load()
  @Override
  public void reload() {
    cache.clear();
    loadCache();
    
    // Cache file probably not existing
    if (cache.isEmpty()) Logger.err("Failed to load addresses from cloud provider '@'! Skipping it...", displayName);
    else Logger.info("Loaded @ addresses for provider '@' from cache file.", cache.size, displayName);
  }

  public abstract Seq<Subnet> downloadList();
  
  
  protected static enum LocalAddressProviderType {
    vpn(0b1000),
    proxy(0b100),
    tor(0b10),
    relay(0b1),
    basic(vpn.val),
    other(0);
    
    public final int val;
    
    LocalAddressProviderType(int val) {
      this.val = val;
    }
    
    public boolean isVPN() { return this == vpn; }
    public boolean isProxy() { return this == proxy; }
    public boolean isTOR() { return this == tor; }
    public boolean isRelay() { return this == relay; }
    public boolean isOther() { return this == other; }
    
    
    // TODO: implement a way to specifies multiple type of provider
  }
}
