package avs.service.providers.custom;

import arc.struct.Seq;
import avs.service.providers.IPValidity;
import avs.util.Logger;
import avs.util.Strings;
import avs.util.PVars;


public class CachedFlaggedAddress extends avs.service.providers.types.CustomAddressProvider {
  private final arc.files.Fi cacheFile;
  private Seq<IPValidity> blacklistedIPs;
  
  public CachedFlaggedAddress() {
    super("Flagged Address Cache", "flagged-address");
    cacheFile = PVars.cacheFolder.child(name + ".txt");
  }

  @Override
  public void load() {
    Seq<IPValidity> ips = new Seq<>();
    
    if (cacheFile.exists() && !cacheFile.isDirectory()) {
      try {
        ips = Seq.with(cacheFile.readString().split("\n")).map(line -> {
          if (line.strip().isBlank()) return null;
          Seq<String> parts = Seq.with(line.strip().split("[\t ]")).map(p -> p.strip()).removeAll(p -> p.isBlank());
          
          IPValidity valid = new IPValidity(parts.get(0));
          if (parts.size > 1 && Strings.isTrue(parts.get(1))) valid.isVPN = true;
          if (parts.size > 2 && Strings.isTrue(parts.get(2))) valid.isProxy = true;
          if (parts.size > 3 && Strings.isTrue(parts.get(3))) valid.isTOR = true;
          if (parts.size > 4 && Strings.isTrue(parts.get(4))) valid.isRelay = true;
          
          return valid;
        }); 
      
      } catch (Exception e) {
        Logger.err("Failed to load cache file '@'. ", cacheFile.path());
        Logger.err("Error: @", e.toString()); 
    }
    } 
    
    ips.removeAll(v -> v == null);
    blacklistedIPs = ips;
    
    // Cache file probably not existing
    if (blacklistedIPs.isEmpty()) Logger.err("Failed to load addresses from flagged IPs cache file! Skipping...");
    else Logger.info("Loaded @ addresses from cache file of already flagged IPs.", blacklistedIPs.size);
  }  
  
  @Override
  public void reloadSettings() {
    load();
  }

  @Override
  public void saveSettings() {
    cacheFile.writeString("");
    blacklistedIPs.each(v -> cacheFile.writeString(Strings.format("@ @ @ @ @",
        v.ip, v.isVPN?1:0, v.isProxy?1:0, v.isTOR?1:0, v.isRelay?1:0) + "\n"));
  }

  @Override
  public void blockAddress(IPValidity address) {
    if(blacklistedIPs.addUnique(address)) saveSettings();
  }

  @Override
  public boolean allowAddress(IPValidity address) {
    boolean removed = blacklistedIPs.remove(address);
    if (removed) saveSettings();
    return removed;
  }

  @Override
  public IPValidity checkIP(String ip) {
    IPValidity.checkIP(ip);
    return blacklistedIPs.find(v -> v.ip.equals(ip));
  }
}
