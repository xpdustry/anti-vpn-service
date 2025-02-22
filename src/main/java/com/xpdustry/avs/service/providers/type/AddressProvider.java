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
import com.xpdustry.avs.util.json.JsonSettings;
import com.xpdustry.avs.util.logging.Logger;

import arc.Events;


public abstract class AddressProvider implements ProviderCategories.Basic {
  protected static final String DESC_BUNDLE_KEY_PREFIX = "avs.provider.description.";

  /** Settings of this provider */
  private JsonSettings settings;
  private boolean loaded = false;
  
  /** Set to false in the constructor if the provider is disabled by default */
  protected boolean enabled = true;
  
  public final String name, displayName;

  /** This will only be used by {@link #exec(Runnable, Logger)} */
  private final Logger defaultLogger; 
  
  protected Logger logger;
  
  
  public AddressProvider(String name) { 
    this(name, arc.util.Strings.capitalize(name)); 
  }
  
  public AddressProvider(String name, String displayName) {
    if (name == null || displayName == null || name.isBlank() || displayName.isBlank()) 
      throw new IllegalArgumentException("name or displayName cannot be null or empty");
      
    this.name = name;
    this.displayName = displayName;
    this.defaultLogger = new Logger(displayName);
    this.logger = this.defaultLogger;
  }
  
  @Override
  public String name() {
    return name;
  }
  
  @Override
  public String displayName() {
    return displayName;
  }
  
  @Override
  public String description() {
    return description(logger);
  }
  
  public String description(Logger logger) {
    return logger.getKey(DESC_BUNDLE_KEY_PREFIX + name());
  }
  
  /** Will temporary replace the provider's logger by the given one, while running the function */
  public void exec(Runnable run, Logger logger) {
    if (logger != null) this.logger = logger;
    try { run.run(); }
    finally { this.logger = this.defaultLogger; }
  }

  @Override
  public boolean load() {
    loaded = false;
    Events.fire(new AVSEvents.ProviderLoadingEvent(this));
    loaded = loadSettings();
    return loaded;
  }

  @Override
  public boolean reload() {
    loaded = false;
    Events.fire(new AVSEvents.ProviderReloadingEvent(this));
    loaded = reloadSettings();
    return loaded;
  }

  @Override
  public boolean save() {
    Events.fire(new AVSEvents.ProviderSavingEvent(this));
    return saveSettings();
  }

  private boolean loadSettings() {
    JsonSettings file = getSettings();
    
    try { 
      file.load();
      
      enabled = file.getBool("enabled", enabled);

      if (!loadMiscSettings()) return false;
      logger.debug("avs.provider.loaded");
      return true;
      
    } catch (Exception e) { 
      logger.err("avs.provider.load-failed", file.file().path());
      logger.err("avs.general-error", e.toString()); 
      return false;
    }
  }
  
  private boolean reloadSettings() {
    logger.debug("avs.provider.reload");
    JsonSettings file = getSettings();
    
    try { 
      file.clear();
      file.load();
      
      enabled = file.getBool("enabled", enabled);

      if (!reloadMiscSettings()) return false;
      logger.debug("avs.provider.loaded");
      return true;
      
    } catch (Exception e) { 
      logger.err("avs.provider.load-failed", file.file().path());
      logger.err("avs.general-error", e.toString()); 
      return false;
    }
  }
  
  private boolean saveSettings() {
    JsonSettings file = getSettings();
    
    try { 
      file.put("enabled", enabled);
      
      if (!saveMiscSettings()) return false;
      logger.debug("avs.provider.saved");
      return true;
      
    } catch(Exception e) {
      logger.err("avs.provider.save-failed", file.file().path());
      logger.err("avs.general-error", e.toString());
      return false;
    } 
  }
  
  protected abstract boolean loadMiscSettings();
  
  protected abstract boolean reloadMiscSettings();
  
  protected abstract boolean saveMiscSettings();
  
  @Override
  public boolean isLoaded() {
    return loaded;
  }
  
  @Override
  public void enable() {
    if (enabled) return;
    logger.info("avs.provider.enabled");
    Events.fire(new AVSEvents.ProviderEnabledEvent(this));
    enabled = true;
    saveSettings();
  }
  
  @Override
  public void disable() {
    if (!enabled) return;
    logger.info("avs.provider.disabled");
    Events.fire(new AVSEvents.ProviderDisabledEvent(this));
    enabled = false;
    saveSettings();
  }
  
  @Override
  public boolean isEnabled() {
    return enabled;
  }
  
  @Override
  public boolean isAvailable() {
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
  
  @Override
  public AddressProviderReply checkAddress(String address) {
    AddressProviderReply reply = new AddressProviderReply(address);
    
    if (!isAvailable()) {
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

  
  protected JsonSettings getSettings() {
    if (settings == null) {
      arc.files.Fi file = AVSConfig.subDir(AVSConfig.providersDirectory.getString()).child(name + ".json");
      settings = new JsonSettings(file, true);
      com.xpdustry.avs.misc.SettingsAutosave.add(settings);
      com.xpdustry.avs.misc.JsonSerializer.apply(settings.getJson());
    }
    
    return settings;
  }
}
