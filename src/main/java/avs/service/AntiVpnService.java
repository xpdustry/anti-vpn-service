package avs.service;

import arc.struct.Seq;

import avs.service.providers.types.*;
import avs.service.providers.IPInfos;
import avs.service.providers.IPValidity;
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
  public static Seq<LocalAddressProvider> 
    // Next we have local lists, downloaded from providers and saved in cache
    // Must be checked at second for more speed reponse
    localProviders = Seq.with(
      new AmazonWebServicesAddressProvider(),
      new AzureAddressProvider(),
      new GithubActionsAddressProvider(),
      new GoogleCloudAddressProvider(),
      new OracleCloudAddressProvider()
  );
  public static Seq<OnlineAddressProvider> 
    // And the online Services API
    // Must be used when local providers founds nothing
    // This list is in rotation: when a service is down or token limit is reach, this will use the next service
    onlineProviders = Seq.with(
      new VpnApiService()
  );
  

  public static IPValidity checkIP(String ip) {
    IPInfos infos = new IPInfos(ip);
    
    
    IPValidity result = new IPValidity(ip, infos);
    
    return result;
  }
  
  public static void loadProviders() {
    
  }

  public static void loadSettins() {
    
  }

  public static void saveSettings() {
    
  }
}
