/*
a * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024 Xpdustry
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

import java.net.InetAddress;

import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.util.DynamicSettings;
import com.xpdustry.avs.util.network.Subnet;

import arc.struct.Seq;
import arc.util.serialization.Json;


public class CachedAddressProvider extends AddressProvider implements ProviderCategories.Cacheable {  
  /** Folder to store the cache file, it's relative to the plugin directory */
  protected String folder;
  
  protected Seq<AddressValidity> cache = new Seq<>(false);
  private DynamicSettings file = null;

  public CachedAddressProvider(String displayName) { 
    super(displayName); 
    folder = AVSConfig.cacheDirectory.get();
  }
  
  public CachedAddressProvider(String displayName, String name) { 
    super(displayName, name);
    folder = AVSConfig.cacheDirectory.get();
  }
  
  @Override
  public AddressValidity get(String subnet) {
    return cache.find(a -> a.subnet.toString().equals(subnet));
  }

  @Override
  public AddressValidity get(Subnet subnet) {
    return cache.find(a -> a.subnet.equals(subnet));
  }
  
  @Override
  public Seq<AddressValidity> matches(String address) {
    return cache.select(s -> s.subnet.isInNet(address));
  }
  
  @Override
  public Seq<AddressValidity> getCache() {
    return cache.copy();
  }
  
  @Override
  public Seq<Subnet> list() {
    return cache.map(a -> a.subnet);
  }
  
  /** Load the cache */
  @SuppressWarnings("unchecked")
  @Override
  protected boolean loadMiscSettings() {
    DynamicSettings file = getCacheFile();
    
    try { 
      file.load();
      
      if (!file.has("cache")) {
        logger.debug("avs.provider.cached.key-not-found", "cache");
        saveMiscSettings();
      }
      
      cache = file.getJson("cache", Seq.class, AddressValidity.class, Seq::new); 
      cache.removeAll(s -> s == null || s.subnet == null || s.type == null);

      if (cache.isEmpty()) logger.warn("avs.provider.cached.empty");
      else logger.info("avs.provider.cached.loaded" + (cache.size > 1 ? "-several" : ""), cache.size);
      return true;
      
    } catch (RuntimeException e) { 
      logger.err("avs.provider.cached.load-failed", file.getFile().path());
      logger.err("avs.general-error", e.toString()); 
      return false;
    }
  }
  
  /** Reload the cache */
  @Override
  protected boolean reloadMiscSettings() {
    logger.info("avs.provider.cached.reload");
    cache.clear();
    getCacheFile().clear();
    return loadMiscSettings();
  }
  
  /** Save the cache */
  @Override
  protected boolean saveMiscSettings() {
    DynamicSettings file = getCacheFile();
    
    try { 
      file.putJson("cache", AddressValidity.class, cache); 
      logger.debug("avs.provider.cached.saved");
      return true;
      
    } catch(RuntimeException e) {
      logger.err("avs.provider.cached.save-failed", file.getFile().path());
      logger.err("avs.general-error", e.toString());
      return false;
    } 
  }

  @Override
  public int cacheSize() {
    return cache.size;
  }

  @Override
  public void checkAddressImpl(AddressProviderReply reply){
    // Normally, never throw an error
    try { 
      InetAddress inet = InetAddress.getByName(reply.address); 
      reply.setResult(cache.find(v -> v.subnet.isInNet(inet)));

    } catch (Exception e) { 
      reply.type = AddressProviderReply.ReplyType.ERROR;
    } 
  }
  
  
  protected DynamicSettings getCacheFile() {
    if (this.file == null) {
      arc.files.Fi file = folder == null ? AVSConfig.subDir(name + ".bin"):
                          AVSConfig.subDir(folder).child(name + ".bin");
      
      this.file = new DynamicSettings(file/*, true*/);
      Json json = new Json();
      com.xpdustry.avs.misc.JsonSerializer.apply(json);
      this.file.setJson(json);
    }
    
    return this.file;
  }
}
