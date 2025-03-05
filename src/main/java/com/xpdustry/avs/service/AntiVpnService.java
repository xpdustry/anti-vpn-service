/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024-2025 Xpdustry
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

package com.xpdustry.avs.service;

import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.misc.AVSEvents;
import com.xpdustry.avs.service.providers.custom.*;
import com.xpdustry.avs.service.providers.local.*;
import com.xpdustry.avs.service.providers.online.*;
import com.xpdustry.avs.service.providers.type.*;
import com.xpdustry.avs.util.logging.Logger;
import com.xpdustry.avs.util.network.Subnet;

import arc.Events;
import arc.struct.Seq;


public class AntiVpnService {
  /**
   * 1- Reversed providers are checked at first. <br>
   * And as his name suggests, the check is valid when the address is found. (like the whitelist)
   * 
   * @apiNote These providers should return null only if the address is not found.
   */
  public static final Seq<AddressProvider> reversedProviders = Seq.with(
      new Whitelist()
  );
  
  /** 2- After, the user editable providers. (and the cache for online providers) */  
  public static final Seq<EditableAddressProvider> customProviders = Seq.with(
      new Blacklist(),
      // Add also the provider used as a cache for online services
      OnlineServiceProvider.cacheProvider
  );
  
  /** 3- Next we have local lists, downloaded from providers and saved in cache. */
  public static final Seq<CloudDownloadedProvider> localProviders = Seq.with(
      new AmazonWebServices(),
      new Azure(),
      new GithubActions(),
      new GoogleCloud(),
      new OracleCloud()
  );
  /**
   * 4- And the online Services API. Will be used when local providers founds nothing. <br><br>
   * This list is in rotation, so when a service is down or token limit is reach, 
   * this will use the next service. <br>
   * Or a random one, if the option was enabled.
   */
  public static final Seq<OnlineServiceProvider> onlineProviders = Seq.with(
      new VpnApi(),
      new IPQualityScore(),
      new IPHub(),
      new IP2Location(),
      new ProxyCheck(),
      new IPApi(),
      new IPApiIs()
  );
  
  /** Field to store all providers */
  public static final Seq<AddressProvider> allProviders = Seq.withArrays(
      reversedProviders, customProviders, localProviders, onlineProviders
  );
  
  private static final Logger logger = new Logger();
  private static boolean operational = false, loaded = false;

  
  public static AddressProvider get(String name) {
    return allProviders.find(p -> p.name.equals(name));
  }
  
  public static AddressProviderReply checkAddress(Subnet address) {
    if (!isOperational()) return null;
    
    AddressProviderReply result;
    Events.fire(new AVSEvents.AddressCheckStartedEvent(address));
    
    for (AddressProvider p : reversedProviders) {
      result = p.checkAddress(address);
      if (result.resultFound()) {
        result.type = AddressProviderReply.ReplyType.ALLOWED;
        return result;
      }
    }

    //TODO: make an option to check providers until one say it's not valid
    
    result = checkAddressImpl(address, customProviders);
    if (result != null && result.resultFound()) return result;
    result = checkAddressImpl(address, localProviders);
    if (result != null && result.resultFound()) return result;
    result = checkAddressOnline(address);

    Events.fire(new AVSEvents.AddressCheckFinishedEvent(address, result));
    return result;
  }
  
  public static AddressProviderReply checkAddressOnline(Subnet address) {
    if (!isOperational()) return null;
    AddressProviderReply result = null, r = null;

    Seq<OnlineServiceProvider> onlines = onlineProviders;
    if (AVSConfig.randomOnlineProviders.getBool() && onlineProviders.size > 1)
      onlines = onlineProviders.copy().shuffle();
    
    for (OnlineServiceProvider p : onlines) {
      if (!isOperational()) return null;
      r = p.checkAddress(address);
      
      if (r.resultFound()) {
        result = r;
        if (!r.validity.type.isNotValid() && !p.isTrusted())
          continue;
        break;
      }   
    }
    
    // Prevents all online services from being unavailable
    if (onlineProviders.allMatch(p -> p.unavailability() != null) && 
        AVSConfig.preventUnavailable.getBool()) {
      logger.warn("avs.service.prevent-unavailable");
      onlineProviders.each(p -> p.makeAvailable());
    }
    
    return result == null ? r : result;
  }
  
  protected static AddressProviderReply checkAddressImpl(Subnet address, Seq<? extends AddressProvider> providers) {
    AddressProviderReply result = null, r = null;
    
    for (AddressProvider p : providers) {
      if (!isOperational()) return null;
      
      r = p.checkAddress(address);
      if (r.resultFound()) {
        result = r;
        break;
      }
    }   

    return result == null ? r : result;
  }
  
  public static boolean isOperational() {
    return operational && loaded;
  }
  
  public static void stop() {
    operational = false;
  }
  
  public static void load() {
    if (loaded) return;
    stop();
    
    logger.info("avs.service.loading");
    AddressProvider err = allProviders.find(p -> !p.load());
    if (err != null) {
      logger.err("avs.service.error", err.displayName, err.name);
      return;
    };
    
    Events.fire(new AVSEvents.ProvidersLoadedEvent());

    // Count the total
    int loadedRanges = 0, localProviders = 0, loadedTokens = 0, onlineProviders = 0;
    
    for (int i=0; i<allProviders.size; i++) {
      AddressProvider p = allProviders.get(i);
      
      if (p instanceof CachedAddressProvider) {
        CachedAddressProvider pc = (CachedAddressProvider) p;
        loadedRanges += pc.cacheSize();
        if (pc.isLoaded()) localProviders++;
      }
      
      if (p instanceof OnlineServiceProvider) {
        OnlineServiceProvider po = (OnlineServiceProvider) p;
        loadedTokens += po.loadedTokens();
        if (po.isLoaded()) onlineProviders++;
      }
    }
    
    logger.none();
    logger.info("avs.service.loaded-local", loadedRanges, localProviders);
    logger.info("avs.service.loaded-online", loadedTokens, onlineProviders);
    logger.none();
    
    loaded = true;
    operational = true;
  }
  
  public static void save() {
    if (loaded) {
      allProviders.each(p -> p.save());
      Events.fire(new AVSEvents.ProvidersSavedEvent());
    } else logger.err("avs.service.cannot-save");
  }
}
