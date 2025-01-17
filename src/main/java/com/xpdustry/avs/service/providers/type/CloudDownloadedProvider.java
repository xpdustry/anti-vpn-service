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

package com.xpdustry.avs.service.providers.type;

import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.misc.AVSEvents;
import com.xpdustry.avs.misc.address.AddressType;
import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.util.network.AdvancedHttp;
import com.xpdustry.avs.util.network.Subnet;

import arc.Events;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;


public abstract class CloudDownloadedProvider extends CachedAddressProvider
                                              implements ProviderCategories.Cloudable {
  public String url;
  /* Define the type of provider, used for statistics. Default is VPN */
  protected ProviderType providerType = ProviderType.vpn;
  /** The headers to use to fetch the list. */
  protected final StringMap headers = new StringMap();
  
  public CloudDownloadedProvider(String name) { super(name); }
  public CloudDownloadedProvider(String name, String displayName) { super(name, displayName); }
  
  @Override
  public boolean refresh() {
    Events.fire(new AVSEvents.CloudProviderRefreshingEvent(this));
    
    Seq<Subnet> list = new Seq<>();
    JsonValue fetched = null;

    // Fetch content
    try { 
      fetched = downloadList();
      if (fetched == null) fetched = downloadFromURL(url);
      
    } catch (Exception e) { 
      logger.warn("avs.provider.cloud.fetch.failed");
      logger.err("avs.general-error", e.toString());
      return false;
    }
    
    if (fetched == null) {
      logger.warn("avs.provider.cloud.fetch.failed");
      return super.loadMiscSettings();
    }
    
    // Extract content to a subnet list
    try {
      list = extractAddressRanges(fetched);
      if (list == null) list = new Seq<>();
      // Remove null and duplicate values
      else list = arc.struct.ObjectSet.with(list).toSeq();
     
    } catch (Exception e) {
      logger.err("avs.provider.cloud.extract-failed");
      logger.warn("avs.provider.cloud.using-cache");
      return super.loadMiscSettings();
    }

    // If fetched list is empty, also use the cache.
    if (list.isEmpty()) {
      logger.warn("avs.provider.cloud.fetch.empty");
      return super.loadMiscSettings();
    }
    
    // Now convert to an AddressValidity
    logger.info("avs.provider.cloud.fetch.found-address" + (list.size > 1 ? "es" : ""), list.size);
    synchronized (cache) {
      cache.clear();
      list.each(s -> {
        AddressValidity valid = new AddressValidity(s);
        valid.type = AddressType.fromBinary(providerType.val);
        cache.add(valid);
      });
      list.clear();      
    }
    
    return saveMiscSettings();    
  }

  @Override
  public boolean loadMiscSettings() {
    if (!AVSConfig.startupDownload.getBool()) {
      logger.warn("avs.provider.cloud.fetch.disabled");
      return super.loadMiscSettings();
    }
    
    return refresh();
  }

  @Override
  public ProviderType providerType() {
    return providerType;
  }
  
  protected JsonValue downloadFromURL(String url) {
    if (url == null || url.isBlank())
      throw new IllegalArgumentException("no url provided");
    
    AdvancedHttp.Reply reply = AdvancedHttp.get(url, headers);
    JsonValue result;
    
    if (reply.isError()) {
      // try to get a "message" key, else ignore and just print the content
      try { reply.setMessage(new JsonReader().parse(reply.message).getString("message")); }
      catch (Exception ignore) {}
      
      logger.err("avs.provider.cloud.fetch.url-failed", url);
      logger.err("avs.http-status", reply.httpStatus, reply.message);
      return null;
    }
    
    try { result = new JsonReader().parse(reply.result); } 
    catch (Exception e) {
      logger.err("avs.provider.cloud.fetch.json-failed", url);
      logger.err("avs.general-error", e.getLocalizedMessage());
      return null;
    }
    
    return result;
  }
  
  /* Override this if your provider have a custom way to get addresses */
  protected JsonValue downloadList() {
    return null;
  }
  
  /* Extract wanted addresses from server reply */
  protected abstract Seq<Subnet> extractAddressRanges(JsonValue downloaded);
  
  
  public static enum ProviderType {
    other,
    vpn,
    proxy,
    tor,
    relay,
    dataCenter;
    
    public final int val;
    
    ProviderType() {
      this.val = 1 << (AddressType.numberOfTypes - ordinal() - 1);
    }
    
    public boolean isOther() { return this == other; }
    public boolean isVPN() { return this == vpn; }
    public boolean isProxy() { return this == proxy; }
    public boolean isTOR() { return this == tor; }
    public boolean isRelay() { return this == relay; }
    public boolean isDataCenter() { return this == dataCenter; }
  }
}
