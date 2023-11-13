package service.providers.types;

import java.net.InetAddress;
import java.net.UnknownHostException;

import arc.files.Fi;
import arc.struct.Seq;
import mindustry.Vars;

import service.IPValidity;
import util.Logger;
import util.Subnet;


public abstract class LocalAddressProvider extends CustomAddressProvider {
  public static Fi cacheFolder = Vars.modDirectory.child("cache");
  
  private final Fi cacheFile;
  private Seq<Subnet> blacklistedIPs;
  
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
    
    blacklistedIPs = downloadList();
    if (blacklistedIPs == null) {
      blacklistedIPs = new Seq<>();
      throw new NullPointerException(".downloadList() should not return null");
    }
    blacklistedIPs.removeAll(v -> v == null);
    
    // If failed to fetch list, use cached list
    if (blacklistedIPs.isEmpty()) {
      Logger.warn("Failed to fetch addresses from cloud provider '@'! Using cached list...", displayName);
      reloadSettings();
      
    } else {
      Logger.info("Found @ addresses for cloud provider '@'. Saving it in cache...", blacklistedIPs.size, displayName);
      saveSettings();
    }
  }
  
  @Override
  public void reloadSettings() {
    Seq<Subnet> ips = new Seq<>();
    
    if (cacheFile.exists() && !cacheFile.isDirectory()) {
      try { ips = Seq.with(cacheFile.readString().split("\n")).map(ip -> ip.strip().isBlank() ? null : Subnet.createInstance(ip.strip())); }
      catch (Exception e) { Logger.err("Failed to load cache file '@'. Error: @", cacheFile.path(), e.toString()); }
    } 
    
    ips.removeAll(v -> v == null);
    blacklistedIPs = ips;
    
    // Cache file probably not existing
    if (blacklistedIPs.isEmpty()) Logger.err("Failed to load addresses from cloud provider '@'! Skipping this provider...", displayName);
    else Logger.info("Loaded @ addresses from provider cache file '@'.", blacklistedIPs.size, displayName);
  }
  
  @Override
  public void saveSettings() {
    cacheFile.writeString(blacklistedIPs.toString("\n"));
  }
  
  @Override
  public void blockIP(String address) {
    if (!blacklistedIPs.contains(s -> s.partialEquals(address))) 
      blacklistedIPs.add(Subnet.createInstance(address));
  }
  
  @Override
  public boolean allowIP(String address) {
    return blacklistedIPs.remove(s -> s.partialEquals(address));
  }
  
  @Override
  public IPValidity checkIP(String ip) {
    IPValidity valid = new IPValidity(ip);
    try {
      InetAddress inet = InetAddress.getByName(ip);
      if (blacklistedIPs.contains(s -> s.isInNet(inet))) valid.isVPN = true;
    } catch (UnknownHostException e) {}
    return valid;
  }
  
  public abstract Seq<Subnet> downloadList();
}
