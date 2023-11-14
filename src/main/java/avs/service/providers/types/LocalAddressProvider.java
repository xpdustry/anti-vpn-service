package avs.service.providers.types;

import arc.files.Fi;
import arc.struct.Seq;

import avs.service.providers.AddressValidity;
import avs.util.Logger;
import avs.util.Subnet;
import avs.util.PVars;


public abstract class LocalAddressProvider extends AddressProvider {
  /* Define the type of provider, used for statistics */
  protected LocalAddressProviderType providerType = LocalAddressProviderType.basic;
  
  public LocalAddressProvider(String displayName, String name) { super(displayName, name); }
  public LocalAddressProvider(String name) { super(name); }

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
        cache.add(valid);
      });
      save();
    }
  }
  
  // Reload only cache. To reload the all, call .load()
  @Override
  public void reload() {
    Fi cacheFile = PVars.cacheFolder.child("cloud").child(name + ".txt");
    Seq<AddressValidity> ips = new Seq<>();
    
    if (cacheFile.exists() && !cacheFile.isDirectory()) {
      try { ips = Seq.with(cacheFile.readString().split("\n")).map(line -> line.isBlank() ? null : AddressValidity.fromString(line.strip())); }
      catch (Exception e) { 
        Logger.err("Failed to load cache file '@'. ", cacheFile.path());
        Logger.err("Error: @", e.toString()); 
      }
    } 
    
    ips.removeAll(v -> v == null);
    cache = ips;
    
    // Cache file probably not existing
    if (cache.isEmpty()) Logger.err("Failed to load addresses from cloud provider '@'! Skipping this provider...", displayName);
    else Logger.info("Loaded @ addresses from provider cache file '@'.", cache.size, displayName);
  }
  
  @Override
  public void save() {
    Fi cacheFile = PVars.cacheFolder.child("cloud").child(name + ".txt");
    cacheFile.writeString("");
    cache.each(s -> cacheFile.writeString(s.toString() + "\n", true));
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
    
    // TODO: implement a way to specifies multiple type of provider
  }
}
