package avs.service.providers.custom;

import avs.service.providers.AddressValidity;
import avs.util.Logger;


public class CachedFlaggedAddress extends avs.service.providers.types.CustomAddressProvider {
  public CachedFlaggedAddress() {
    super("Flagged Address Cache", "flagged-address");
  }

  @Override
  protected void loadCache() {
    super.loadCache();
    // Little check because already flagged IPs are without mask, otherwise it would be a subnet.
    // So check if doen't have a mask and it's not a network IP or broadcast IP. 
    try { 
      cache.each(v -> {
        try { AddressValidity.checkIP(v.ip.toString()); }
        catch (Exception e) { 
          Logger.err("Failed to load flagged IPs cache file. (ip: @)", v.ip.toString());
          Logger.err("Error: @", e.toString());
          // re-throw error to clear cache after
          throw e;
        }
      }); 
    } catch (Exception e) { cache.clear(); }
  }  
}
