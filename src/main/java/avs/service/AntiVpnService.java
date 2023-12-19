package avs.service;

import arc.struct.Seq;

import avs.util.Logger;
import avs.util.PVars;
import avs.service.providers.*;
import avs.service.providers.types.*;
import avs.service.providers.custom.*;
import avs.service.providers.local.*;
import avs.service.providers.online.*;


public class AntiVpnService {
  public static Seq<CustomAddressProvider> 
    // Customs providers are whitelist, already flagged ips cache, and the custom user list
    // These providers must be checked at first
    customProviders = Seq.with(
      new WhitelistAddress(),
      new UserAddressList(),
      new CachedFlaggedAddress()
  );
  public static Seq<CloudDownloadedAddressProvider> 
    // Next we have local lists, downloaded from providers and saved in cache
    // Must be checked at second for more speed reponse
    localProviders = Seq.with(
      new AmazonWebServicesAddressProvider(),
      new AzureAddressProvider(),
      new GithubActionsAddressProvider(),
      new GoogleCloudAddressProvider(),
      new OracleCloudAddressProvider()
  );
  public static Seq<OnlineServiceAddressProvider> 
    // And the online Services API
    // Must be used when local providers founds nothing
    // This list is in rotation: when a service is down or token limit is reach, this will use the next service
    onlineProviders = Seq.with(
      new VpnApiService()
  );
  
  // These providers are used, and need to be global
  public static CustomAddressProvider whitelist, flaggedCache;
  // Logger without a topic is by default the main plugin topic
  public static Logger logger = new Logger();
  public static boolean operational = false;
  public static int totalAddresses = 0, totalLocalProviders = 0;
  

  public static AddressValidity checkIP(String ip) {
    AddressValidity.checkIP(ip);
    AddressValidity result;
    
    // Whitelist is a special provider, the check is inverted
    
    

    return null;
  }
  
  public static void loadProviders() {
    operational = false;
    
    customProviders.each(p -> p.load());
    localProviders.each(p -> p.load());
    onlineProviders.each(p -> p.load());
    
    // TODO: print total count
    
    // Set lists used by other providers
    whitelist = customProviders.find(p -> p.name.equals(PVars.whitelistProviderName));
    if (whitelist == null) logger.warn("Unable to find whitelist provider.");
    flaggedCache = customProviders.find(p -> p.name.equals(PVars.flaggedCacheProviderName));
    if (flaggedCache == null) logger.warn("Unable to find flagged IPs cache.");

    totalAddresses = 0;
    totalLocalProviders = 0;
    customProviders.each(p -> {
      totalAddresses += p.cacheSize();
      totalLocalProviders++;
    });
    localProviders.each(p -> {
      totalAddresses += p.cacheSize();
      totalLocalProviders++;
    });
    logger.info("Loaded @ addresses ranges from @ providers", totalAddresses, totalLocalProviders);
    
    operational = true;
  }
  
  public static void saveProviders() {
    if (!operational) return;
    
    customProviders.each(p -> p.save());
    localProviders.each(p -> p.save());
    onlineProviders.each(p -> p.save());
  }
}
