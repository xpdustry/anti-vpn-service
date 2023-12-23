/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2023 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package avs.service;

import arc.struct.Seq;

import avs.util.Logger;
import avs.util.address.AddressValidity;
import avs.config.PVars;
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
      new CustomBlacklistAddressProvider(),
      new CachedFlaggedAddress()
  );
  public static Seq<CloudDownloadedAddressProvider> 
    // Next we have local lists, downloaded from providers and saved in cache
    // Must be checked at second for more speed response
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
    if (whitelist != null) {
      result = whitelist.checkIP(ip);
      if (result != null) {
        logger.debug("Ignoring this ip, because it is whitelisted");
        return null;
      }
    }
    
    result = checkIP(ip, customProviders, p -> !p.name.equals(PVars.whitelistProviderName));
    if (result != null && result.type.isNotValid()) return result;
    result = checkIP(ip, localProviders);
    if (result != null && result.type.isNotValid()) return result;
    result = checkIP(ip, onlineProviders);
    
    return result;
  }
  
  public static AddressValidity checkIP(String ip, Seq<? extends AddressProvider> providers) { return checkIP(ip, providers, null); }
  public static AddressValidity checkIP(String ip, Seq<? extends AddressProvider> providers, arc.func.Boolf<AddressProvider> predicate) {
    AddressValidity result;
    
    for (int i=0; i<providers.size; i++) {
      if (predicate != null && !predicate.get(providers.items[i])) continue;
      result = providers.items[i].checkIP(ip);
      if (result != null && result.type.isNotValid()) return result;
    }
    
    return null;
  }
  
  public static void loadProviders() {
    operational = false;
    
    customProviders.each(p -> p.load());
    localProviders.each(p -> p.load());
    onlineProviders.each(p -> p.load());

    // Set lists used by other providers
    whitelist = customProviders.find(p -> p.name.equals(PVars.whitelistProviderName));
    if (whitelist == null) logger.warn("Unable to find whitelist provider.");
    flaggedCache = customProviders.find(p -> p.name.equals(PVars.flaggedCacheProviderName));
    if (flaggedCache == null) logger.warn("Unable to find flagged IPs cache.");

    // Count the total
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
