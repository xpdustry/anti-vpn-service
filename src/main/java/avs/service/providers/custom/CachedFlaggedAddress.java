package avs.service.providers.custom;

import arc.files.Fi;
import arc.struct.Seq;

import avs.service.providers.AddressValidity;
import avs.util.Logger;
import avs.util.PVars;


public class CachedFlaggedAddress extends avs.service.providers.types.CustomAddressProvider {
  public CachedFlaggedAddress() {
    super("Flagged Address Cache", "flagged-address");
  }

  @Override
  public void load() {
    Fi cacheFile = PVars.cacheFolder.child(name + ".txt");
    Seq<AddressValidity> ips = new Seq<>();
    
    if (cacheFile.exists() && !cacheFile.isDirectory()) {
      try { 
        ips = Seq.with(cacheFile.readString().split("\n")).map(line -> {
          if (line.isBlank()) return null;
          AddressValidity valid = AddressValidity.fromString(line.strip());
          // Little check because already flagged IPs are without mask, otherwise it would be a subnet.
          // So check if doen't have a mask and it's not a network IP or broadcast IP.
          AddressValidity.checkIP(valid.ip.toString());
          return valid;
        }); 
      } catch (Exception e) { 
        Logger.err("Failed to load cache file '@'. ", cacheFile.path());
        Logger.err("Error: @", e.toString()); 
      }
    } 
    
    ips.removeAll(v -> v == null);
    cache = ips;
    
    // Cache file probably not existing
    if (cache.isEmpty()) Logger.err("Failed to load addresses from flagged IPs cache file! Skipping...");
    else Logger.info("Loaded @ addresses from cache file of already flagged IPs.", cache.size);
  }  

  @Override
  public void save() {
    Fi cacheFile = PVars.cacheFolder.child(name + ".txt");
    cacheFile.writeString("");
    cache.each(s -> cacheFile.writeString(s.toString() + "\n", true));
  }
}
