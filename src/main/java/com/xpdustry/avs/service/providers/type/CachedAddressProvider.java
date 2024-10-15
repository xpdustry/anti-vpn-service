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

import com.xpdustry.avs.misc.AVSConfig;
import com.xpdustry.avs.misc.JsonSerializer;
import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.util.DynamicSettings;

import arc.struct.Seq;
import arc.util.serialization.Json;


public class CachedAddressProvider extends AddressProvider {
  protected Seq<AddressValidity> cache = new Seq<>(false);
  protected DynamicSettings cacheFile = null;
  protected String cacheKey = "cache";

  public CachedAddressProvider(String displayName) { 
    super(displayName); 
    folder = AVSConfig.cacheDirectory.get();
  }
  
  public CachedAddressProvider(String displayName, String name) { 
    super(displayName, name);
    folder = AVSConfig.cacheDirectory.get();
  }

  @Override
  public boolean load() {
    loaded = false;
    loaded = loadCache();
    return loaded;
  }

  @Override
  public boolean reload() {
    loaded = false;
    logger.info("avs.provider.cached.reload");
    cache.clear();
    getCacheFile().clear();
    return load();
  }
  
  @Override
  public boolean save() {
    return saveCache();
  }
  
  public int cacheSize() {
    return cache.size;
  }

  @SuppressWarnings("unchecked")
  protected boolean loadCache() {
    DynamicSettings file = getCacheFile();
    
    try { 
      file.load();
      
      if (!file.has(cacheKey)) {
        logger.debug("avs.provider.cached.key-not-found", cacheKey);
        saveCache();
      }
      
      cache = file.getJson(cacheKey, Seq.class, AddressValidity.class, Seq::new); 
      cache.removeAll(s -> s == null || s.subnet == null || s.type == null);
      
      if (cache.isEmpty()) logger.warn("avs.provider.cached.empty");
      else logger.info("avs.provider.cached.loaded" + (cache.size > 1 ? "-several" : ""), cache.size);
      
    } catch (Exception e) { 
      logger.err("avs.provider.cached.load-failed", file.getFile().path());
      logger.err("avs.general-error", e.toString()); 
      return false;
    }

    return true;
  }
  
  protected boolean saveCache() {
    DynamicSettings file = getCacheFile();
    
    try { 
      file.putJson(cacheKey, AddressValidity.class, cache); 
      logger.debug("avs.provider.cached." + (file.isModified() ? "saved" : "no-modifications"));
    } catch(Exception e) {
      logger.err("avs.provider.cached.save-failed", file.getFile().path());
      logger.err("avs.general-error", e.toString());
      return false;
    }
    return true;
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
    if (cacheFile == null) {
      //fileExt = "json";
      //cacheFile = new DynamicSettings(getFile(), true);
      cacheFile = new DynamicSettings(getFile());
      cacheFile.setErrorHandler(logger::err);
      Json json = new Json();
      JsonSerializer.apply(json);
      cacheFile.setJson(json);
    }
    
    return cacheFile;
  }
}
