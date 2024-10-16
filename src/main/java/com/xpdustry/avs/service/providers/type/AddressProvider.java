/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
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

import com.xpdustry.avs.misc.AVSConfig;
import com.xpdustry.avs.misc.AVSEvents;
import com.xpdustry.avs.util.Logger;

import arc.Events;
import arc.files.Fi;


public abstract class AddressProvider {
  /** Folder to store the file, it's relative to the plugin directory */
  protected String folder;
  /** This field can be set manually, if desired. */
  protected Fi file;
  protected String fileExt;
  
  public final String name, displayName;
  protected final Logger logger;
  
  protected boolean enabled = true;
  /** Must be manually set to {@code true} by the inherited class, when overriding {@link #load()} */
  protected boolean loaded = false;
  
  public AddressProvider(String displayName) { 
    this(displayName, displayName.toLowerCase().replace(' ', '-').replace(',', '_')); 
  }
  
  public AddressProvider(String displayName, String name) {
    if (name == null || displayName == null || name.isBlank() || displayName.isBlank()) 
      throw new IllegalArgumentException("name or displayName cannot be null or empty");
      
    this.name = name;
    this.displayName = displayName;
    this.logger = new Logger(displayName);
  }
  
  public abstract boolean load();
  
  public abstract boolean reload();
  
  public abstract boolean save();
  
  /**
   * Check if address is blacklisted.
   * 
   * @apiNote the result is never {@code null}.
   */
  public AddressProviderReply checkAddress(String address) {
    AddressProviderReply reply = new AddressProviderReply(address);
    
    if (!isProviderAvailable()) {
      reply.type = AddressProviderReply.ReplyType.UNAVAILABLE;
      return reply;
    }
    
    try {
      Events.fire(new AVSEvents.ProviderCheckingAddressEvent(this, address));
      logger.debug("avs.provider.checking", address);
      checkAddressImpl(reply);
      Events.fire(new AVSEvents.ProviderCheckedAddressEvent(this, address, reply));
      
      if (reply.validity != null) 
        logger.debug("avs.provider.check." + (reply.validity.type.isNotValid() ? "invalid" : "valid"));

    } catch (Exception e) {
      Events.fire(new AVSEvents.ProviderCheckingAddressFailedEvent(this, address, e));
      logger.debug("avs.provider.check.failed", address);
      logger.debug("avs.general-error", e.toString()); 
      reply.type = AddressProviderReply.ReplyType.ERROR;
    }
    
    return reply;
  }
  
  protected abstract void checkAddressImpl(AddressProviderReply reply);
  
  public boolean isLoaded() {
    return loaded;
  }
  
  public void enable() {
    if (enabled) return;
    Events.fire(new AVSEvents.ProviderEnabledEvent(this));
    enabled = true;
  }
  
  public void disable() {
    if (!enabled) return;
    Events.fire(new AVSEvents.ProviderDisabledEvent(this));
    enabled = false;
  }
  
  public boolean isEnabled() {
    return enabled;
  }
  
  public boolean isProviderAvailable() {
    if (!isEnabled()) {
      logger.debug("avs.provider.disabled");
      return false;
    }
    
    if (!isLoaded()) {
      logger.err("avs.provider.not-ready");
      return false;
    }
    
    return true;
  }

  protected Fi getFile() {
    if (fileExt == null) fileExt = AVSConfig.defaultCacheFileExt.get();
    
    if (file == null) {
      if (folder == null) file = AVSConfig.subDir(name + "." + fileExt);
      else file = AVSConfig.subDir(folder).child(name + "." + fileExt);
    }
    
    return file;
  }
}
