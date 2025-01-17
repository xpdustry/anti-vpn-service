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

package com.xpdustry.avs.config.abstracts;

import com.xpdustry.avs.util.DynamicSettings;
import com.xpdustry.avs.util.Logger;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Strings;


public abstract class AbstractConfig {
  public final String name;
  public final boolean plainJson;
  public final Seq<AbstractField<?>> all = new Seq<>();
  
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
  
  public AbstractField<?> get(String name) {
    return all.find(f -> f.name().equals(name));
  }
  
  public void notifyValuesChanged() {
    all.each(f -> {
      try { f.validateChange(logger); }
      catch (Exception e) { // probably invalid value in config file
        // notify, remove the field and use the default value
        logger.err(msgBundleKey("invalid-field"), f.name());
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
  
  protected abstract String msgBundleKey(String key);
  
  public void load() {
    isLoading = true;
    
    logger.info(msgBundleKey("loading"));
    Fi file = getFile();
    logger.debug(msgBundleKey("file"), file.absolutePath());
    
    config = new DynamicSettings(file, true);
    config.load();
    loadMisc();

    isLoading = false;
  }
  
  /** Override this if other things must also be loaded */
  protected void loadMisc() {}
  
  protected abstract Fi getFile();
}
