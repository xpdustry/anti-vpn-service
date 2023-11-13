package service;

import arc.struct.Seq;

import service.providers.types.*;
import service.providers.local.*;
import service.providers.online.*;


public class AntiVpnService {
  private static Seq<CustomAddressProvider> 
    // Customs providers are whitelist, already flagged ips cache, and the custom user list
    // These providers must be checked at first
    customProviders = Seq.with(
  );
  private static Seq<LocalAddressProvider> 
    // Next we have local lists, downloaded from providers and saved in cache
    // Must be checked at second for more speed reponse
    localProviders = Seq.with(
      
  );
  private static Seq<OnlineAddressProvider> 
    // And the online Services API
    // Must be used when local providers founds nothing
    // This list is in rotation: when a service is down or token limit is reach, this will use the next service
    onlineProviders = Seq.with(
      
  );
  

  public static IPValidity checkIP(String ip) {
    IPInfos infos = new IPInfos(ip);
    
    
    IPValidity result = new IPValidity(ip, infos);
    
    return result;
  }
  
  public static void downloadProvidersLists() {
    
  }

  public static void loadSettins() {
    
  }

  public static void saveSettings() {
    
  }
}
