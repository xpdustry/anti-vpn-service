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

package com.xpdustry.avs.config.base;

import com.xpdustry.avs.util.json.DynamicSettings;
import com.xpdustry.avs.util.logging.Logger;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Strings;


public abstract class AbstractConfig {
  public final String name;
  public final boolean plainJson;
  public final Seq<IField<?>> all = new Seq<>();
  
  protected DynamicSettings config;  
  protected boolean isLoading;
  protected final Logger logger;
  
  public AbstractConfig(String name) {
    this(name, true);
  }
  
  public AbstractConfig(String name, boolean plainJson) {
    this.name = name;
    this.plainJson = plainJson;
    this.logger = new Logger(Strings.capitalize(name));
  }
  
  public IField<?> get(String name) {
    return all.find(f -> f.name().equals(name));
  }
  
  public void notifyValuesChanged() {
    all.each(f -> {
      try { f.validateChange(logger); }
      catch (Exception e) { // probably invalid value in config file
        // notify, remove the field and use the default value
        logger.err("avs.aconfig.invalid-field", f.name());
        logger.err("avs.general-error", e.toString());
        f.setDefault(logger);
      }
    });
  }
    
  public DynamicSettings config() {
    return config;
  }
  
  public boolean isLoaded() {
    return config != null && !isLoading;
  }

  /** Used to format the key to get the field description from bundle */
  protected abstract String getFieldDescKey(IField<?> field);
  
  public void load() {
    isLoading = true;
    
    logger.info("avs.aconfig.loading");
    Fi file = getFile();
    logger.debug("avs.aconfig.file", file.absolutePath());
    
    try {
      config = new DynamicSettings(file, true);
      com.xpdustry.avs.misc.JsonSerializer.apply(config.getJson());
      config.load();

      all.each(s -> s instanceof CachedField, s -> ((CachedField<?>) s).load());
      loadMisc();
      
    } catch (Exception e) {
      logger.err("avs.aconfig.load-failed");
      logger.err("avs.general-error", e.toString());
      config = null;
      return;
    }
    
    isLoading = false;
  }
  
  /** Only useful for saving the {@link CachedField} fields content. */
  public boolean save() {
    try { 
      all.each(s -> s instanceof CachedField, s -> ((CachedField<?>) s).save());
      logger.info("avs.aconfig.saved");
      return true;
    
    } catch (Exception e) {
      logger.err("avs.aconfig.save-failed");
      logger.err("avs.general-error", e.toString());
      return false;
    }
  }
  
  /** Can be overrides to load other things. */
  protected void loadMisc() {}
  
  protected abstract Fi getFile();
}
