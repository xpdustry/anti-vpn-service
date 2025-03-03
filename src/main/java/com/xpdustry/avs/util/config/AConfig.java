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

package com.xpdustry.avs.util.config;

import com.xpdustry.avs.util.json.JsonSettings;
import com.xpdustry.avs.util.logging.Logger;

import arc.files.Fi;
import arc.struct.Seq;


public abstract class AConfig {
  public final String name;
  public final boolean plainJson;
  public final Seq<Field<?>> all = new Seq<>();
  
  protected JsonSettings config;  
  protected boolean isLoading;
  protected final Logger logger;
  
  public AConfig(String name) {
    this(name, true);
  }
  
  public AConfig(String name, boolean plainJson) {
    this.name = name;
    this.plainJson = plainJson;
    this.logger = new Logger(arc.util.Strings.capitalize(name));
  }

  public Field<?> get(String name) {
    return all.find(f -> f.name.equals(name));
  }
  
  public void notifyValuesChanged() {
    all.each(f -> {
      try { f.validateChange(logger); }
      catch (Exception e) { // probably invalid value in config file
        // notify, remove the field and use the default value
        logger.err("avs.aconfig.invalid-field", f.name);
        logger.err("avs.general-error", e.toString());
        f.setDefault(logger);
      }
    });
  }
    
  public JsonSettings config() {
    return config;
  }
  
  public boolean isLoaded() {
    return config != null && !isLoading;
  }

  /** Used to format the key to get the field description from bundle */
  protected abstract String getFieldDescKey(Field<?> field);
  
  public void load() {
    isLoading = true;
    
    logger.info("avs.aconfig.loading");
    Fi file = getFile();
    logger.debug("avs.aconfig.file", file.absolutePath());
    
    try {
      config = new JsonSettings(file, plainJson);
      com.xpdustry.avs.misc.SettingsAutosave.add(config);
      com.xpdustry.avs.misc.JsonSerializer.apply(config.getJson());
      config.load();

      all.each(f -> f.load());
      loadMisc();
      
    } catch (Exception e) {
      logger.err("avs.aconfig.load-failed");
      logger.err("avs.general-error", e.toString());
      config = null;
      return;
    }
    
    isLoading = false;
  }

  public boolean save() {
    try { 
      all.each(f -> f.save());
      logger.info("avs.aconfig.saved");
      return true;
    
    } catch (Exception e) {
      logger.err("avs.aconfig.save-failed");
      logger.err("avs.general-error", e.toString());
      return false;
    }
  }
  
  public void reload() {
    load();
    if (isLoaded()) {
      notifyValuesChanged();
      logger.info("avs.aconfig.reloaded");
    }
  }
  
  /** Can be overrides to load other things. */
  protected void loadMisc() {}
  
  protected abstract Fi getFile();
}
