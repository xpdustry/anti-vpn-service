package avs.service.providers.types;

import arc.struct.Seq;
import arc.util.serialization.JsonValue;
import avs.config.PVars;
import avs.util.address.AddressValidity;
import avs.util.network.AwaitHttp;
import avs.util.network.Subnet;


public abstract class CloudDownloadedAddressProvider extends AddressProvider {
  public final String url;
  /* Define the type of provider, used for statistics. Default is VPN */
  protected CloudAddressProviderType providerType = CloudAddressProviderType.basic;
  
  private boolean loaded;
  private Seq<Subnet> list;
  private Throwable error;
  
  public CloudDownloadedAddressProvider(String displayName, String name, String url) { 
    super(displayName, name); 
    customFolder = PVars.cacheFolder.child("cloud");
    if (url == null || url.isBlank()) throw new NullPointerException("url is empty");
    this.url = url.strip();
  }
  public CloudDownloadedAddressProvider(String name, String url) { 
    super(name); 
    customFolder = PVars.cacheFolder.child("cloud");
    if (url == null || url.isBlank()) throw new NullPointerException("url is empty");
    this.url = url.strip();
  }

  @Override
  public boolean load() {
    loaded = false;
    list = new Seq<>();
    error = new Exception("unknown error");

    try { 
      JsonValue fetched = downloadList();
      
      if (fetched != null) {
        list = extractAddressRanges(fetched);
        loaded = true;
        
      } else {
        AwaitHttp.get(url, success -> {
          if (success.getStatus() == AwaitHttp.HttpStatus.OK) {
            list.addAll(extractAddressRanges(new arc.util.serialization.JsonReader().parse(success.getResultAsString().strip())));
            loaded = true;
            
          } else error = new Exception(success.getStatus().toString().replace('_', ' '));
        }, failure -> error = failure);
      }
      
    } catch (Exception e) { error = e; }
    
    if (list == null) list = new Seq<>();
    list.removeAll(v -> v == null);
    
    // If failed to fetch list, use cached list
    if (list.isEmpty()) {
      if (loaded) {
        logger.warn("Fetched list is empty. Using cached list...");
      } else {
        logger.err("Failed to fetch addresses!");
        logger.err("Error: " + error.getLocalizedMessage());
        logger.warn("Using cached list...");
      }
      return reload();
    }
    
    logger.info("Found @ address" + (list.size > 1 ? "es" : "") + ". Saving it in cache...", list.size);
    cache.clear();
    list.each(s -> {
      AddressValidity valid = new AddressValidity(s);
      valid.type.vpn = providerType.isVPN();
      valid.type.proxy = providerType.isProxy();
      valid.type.tor = providerType.isTOR();
      valid.type.relay = providerType.isRelay();
      valid.type.other = providerType.isOther();
      
      cache.add(valid);
    });
    list.clear();
    
    return save();
  }
  
  // Reload only cache. To reload the all, call .load()
  @Override
  public boolean reload() {
    cache.clear();
    boolean loaded = loadCache();
    cache.removeAll(s -> s == null || s.ip == null || s.type == null);
    
    // Cache file probably not existing
    if (!loaded) logger.err("Failed to load addresses! Skipping it...");
    else if (cache.isEmpty()) logger.warn("No addresses found.");
    else logger.info("Loaded @ address" + (cache.size > 1 ? "es" : "") + " from cache file.", cache.size);
    
    return loaded;
  }

  /* Redefine this if your provider have a custom way to get addresses */
  public JsonValue downloadList() {
    return null;
  }
  /* Extract wanted addresses from server reply */
  public abstract Seq<Subnet> extractAddressRanges(JsonValue downloaded);
  
  
  public static enum CloudAddressProviderType {
    vpn(0b1000),
    proxy(0b100),
    tor(0b10),
    relay(0b1),
    basic(vpn.val),
    other(0);
    
    public final int val;
    
    CloudAddressProviderType(int val) {
      this.val = val;
    }
    
    public boolean isVPN() { return this.val == vpn.val; }
    public boolean isProxy() { return this.val == proxy.val; }
    public boolean isTOR() { return this.val == tor.val; }
    public boolean isRelay() { return this.val == relay.val; }
    public boolean isOther() { return this.val == other.val; }
    
    
    // TODO: implement a way to specifies multiple type of provider
  }
}
