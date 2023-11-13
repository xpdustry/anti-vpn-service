package avs.service.providers.types;

import java.net.InetAddress;

import arc.files.Fi;
import arc.struct.Seq;

import avs.service.IPValidity;
import avs.util.Logger;
import avs.util.Subnet;
import avs.util.PVars;


public abstract class LocalAddressProvider extends AddressProvider {
  private static Fi cacheFolder = PVars.cacheFolder.child("cloud");
  
  private final Fi cacheFile;
  private Seq<Subnet> blacklist;
  
  public LocalAddressProvider(String displayName, String name) { 
    super(displayName, name); 
    cacheFile = cacheFolder.child(name + ".txt");
  }
  
  public LocalAddressProvider(String name) { 
    super(name);
    cacheFile = cacheFolder.child(name + ".txt");
  }

  @Override
  public void load() {
    
    blacklist = downloadList();
    if (blacklist == null) {
      blacklist = new Seq<>();
      throw new NullPointerException(".downloadList() should not return null");
    }
    blacklist.removeAll(v -> v == null);
    
    // If failed to fetch list, use cached list
    if (blacklist.isEmpty()) {
      Logger.warn("Failed to fetch addresses from cloud provider '@'! Using cached list...", displayName);
      reloadCache();
      
    } else {
      Logger.info("Found @ addresses for cloud provider '@'. Saving it in cache...", blacklist.size, displayName);
      saveCache();
    }
  }
  
  public void reloadCache() {
    Seq<Subnet> ips = new Seq<>();
    
    if (cacheFile.exists() && !cacheFile.isDirectory()) {
      try { ips = Seq.with(cacheFile.readString().split("\n")).map(ip -> ip.strip().isBlank() ? null : Subnet.createInstance(ip.strip())); }
      catch (Exception e) { 
        Logger.err("Failed to load cache file '@'. ", cacheFile.path());
        Logger.err("Error: @", e.toString()); 
      }
    } 
    
    ips.removeAll(v -> v == null);
    blacklist = ips;
    
    // Cache file probably not existing
    if (blacklist.isEmpty()) Logger.err("Failed to load addresses from cloud provider '@'! Skipping this provider...", displayName);
    else Logger.info("Loaded @ addresses from provider cache file '@'.", blacklist.size, displayName);
  }
  
  public void saveCache() {
    cacheFile.writeString("");
    blacklist.each(s -> cacheFile.writeString(s.toString() + "\n", true));
  }
  
  @Override
  public IPValidity checkIP(String ip) {
    try {
      IPValidity.checkIP(ip);
      IPValidity valid = new IPValidity(ip);      
      InetAddress inet = InetAddress.getByName(ip);
      
      if (blacklist.contains(s -> s.isInNet(inet))) valid.isVPN = true;
      return valid;
    } catch (Exception e) {
      Logger.debug("Failed to check ip '@'", ip);
      Logger.debug("Error: @", e.toString()); 
    }
    
    return null;
  }
  
  public abstract Seq<Subnet> downloadList();
}
