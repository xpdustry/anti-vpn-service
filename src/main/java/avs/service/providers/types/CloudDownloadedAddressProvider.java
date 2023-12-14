package avs.service.providers.types;

import arc.struct.Seq;

import avs.service.providers.AddressValidity;
import avs.util.PVars;
import avs.util.Subnet;


public abstract class CloudDownloadedAddressProvider extends AddressProvider {
  /* Define the type of provider, used for statistics. Default is VPN */
  protected LocalAddressProviderType providerType = LocalAddressProviderType.basic;
  
  public CloudDownloadedAddressProvider(String displayName, String name) { 
    super(displayName, name); 
    customFolder = PVars.cacheFolder.child("cloud");
  }
  public CloudDownloadedAddressProvider(String name) { 
    super(name); 
    customFolder = PVars.cacheFolder.child("cloud");
  }

  @Override
  public boolean load() {
    boolean loaded = true;
    Seq<Subnet> list;
    Exception error = new NullPointerException();
    
    try { list = downloadList(); }
    catch (Exception e) {
      list = new Seq<>();
      loaded = false;
      error = e;
    }
    
    if (list == null) {
      list = new Seq<>();
      loaded = false;
    }
    
    list.removeAll(v -> v == null);
    
    // If failed to fetch list, use cached list
    if (list.isEmpty()) {
      if (loaded) {
        logger.warn("Fetched list is empty. Using cached list...");
      } else {
        logger.err("Failed to fetch addresses!");
        logger.err("Error: " + error.toString());
        logger.warn("Using cached list...");
      }
      return reload();
    }
    
    logger.info("Found @ address" + (list.size > 1 ? "es" : "") + ". Saving it in cache...", list.size);
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
    
    return save();
  }
  
  // Reload only cache. To reload the all, call .load()
  @Override
  public boolean reload() {
    cache.clear();
    getCacheFile().clear();
    boolean loaded = loadCache();
    
    // Cache file probably not existing
    if (!loaded) logger.err("Failed to load addresses! Skipping it...");
    else if (cache.isEmpty()) logger.debug("No addresses found.");
    else logger.info("Loaded @ address" + (cache.size > 1 ? "es" : "") + " from cache file.", cache.size);
    
    return loaded;
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
