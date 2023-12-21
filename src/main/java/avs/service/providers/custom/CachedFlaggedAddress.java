package avs.service.providers.custom;


public class CachedFlaggedAddress extends avs.service.providers.types.CustomAddressProvider {
  public CachedFlaggedAddress() {
    super("Flagged Address Cache", avs.config.PVars.flaggedCacheProviderName);
  }

  @Override
  protected boolean loadCache() {
    if (!super.loadCache()) return false;
    
    // Little check because already flagged IPs are without mask, otherwise it would be a subnet.
    // So check if doen't have a mask and it's not a network IP or broadcast IP. 
    try { 
      cache.each(v -> {
        try { avs.util.address.AddressValidity.checkIP(v.ip.toString()); }
        catch (Exception e) { 
          logger.err("Failed to load flagged IPs cache file. (ip: @)", v.ip.toString());
          logger.err("Error: @", e.toString());
          // re-throw error to clear cache after
          throw e;
        }
      }); 
      
    } catch (Exception e) { 
      cache.clear(); 
      return false;
    }
    return true;
  }  
}
