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

package com.xpdustry.avs.config;

import com.xpdustry.avs.util.Logger;


public class ConfigField {
  public static final String bundleDescPrefix = "avs.config.";
  
  public final String name;
  public final Object defaultValue;
  public final boolean isDev, readOnly;
  protected final ConfigEvents.Callback changed;
  
  ConfigField(String name, Object defaultValue) {
    this(name, defaultValue, null, false, false);
  }
  
  ConfigField(String name, Object defaultValue, boolean isDev) {
    this(name, defaultValue, null, isDev, false);
  }
  
  ConfigField(String name, Object defaultValue, boolean isDev, boolean readOnly) {
    this(name, defaultValue, null, isDev, readOnly);
  }

  ConfigField(String name, Object defaultValue, ConfigEvents.Callback changed) {
    this(name, defaultValue, changed, false, false);
  }

  ConfigField(String name, Object defaultValue, ConfigEvents.Callback changed, boolean isDev) {
    this(name, defaultValue, changed, isDev, false);
  }

  ConfigField(String name, Object defaultValue, ConfigEvents.Callback changed, boolean isDev, boolean readOnly) {
    this.name = name;
    if (defaultValue == null) throw new NullPointerException(name + ": defaultValue cannot be null");
    this.defaultValue = defaultValue;
    this.isDev = isDev;
    this.readOnly = readOnly;
    this.changed = changed == null ? (v, l) -> true : changed;

    AVSConfig.all.add(this);
  }
  
  protected void checkType(Object o) {
    if (o == null) throw new NullPointerException(name + ": value to check is null");
    if (o.getClass() != defaultValue.getClass())
      throw new IllegalArgumentException(name + ": incompatible type " + o.getClass().getName() + 
                                         ". Must be a " + defaultValue.getClass().getName());
  }

  public boolean isInt(){
    return defaultValue instanceof Integer;
  }
  
  public boolean isFloat(){
    return defaultValue instanceof Float;
  }
  
  public boolean isBool(){
    return defaultValue instanceof Boolean;
  }
  
  public boolean isString(){
    return defaultValue instanceof String;
  }
  
  @SuppressWarnings("unchecked")
  public <T> T get() {
    // Allows getting values ​​if configuration is loading.
    if (!AVSConfig.isLoading && !AVSConfig.isLoaded()) 
      throw new IllegalStateException("config is not loaded. Use AVSConfig.load() first.");
    
    if (AVSConfig.config == null) return (T) defaultValue;
    T value = (T) AVSConfig.config.get(name, defaultValue);
    checkType(value);
    return value;
  }
  
  
  public int getInt() {
    return get();
  } 
  
  public float getFloat() {
    return get();
  }
  
  public boolean getBool() {
    return get();
  }
  
  public String getString() {
    return get();
  }
  
  public boolean set(Object value) { return set(value, AVSConfig.logger); }
  /** Return whether the change has been accepted or not */
  public boolean set(Object value, Logger logger) {
    if (!AVSConfig.isLoaded()) throw new IllegalStateException("config is not loaded. Use AVSConfig.load() first.");
    if (readOnly) return false;
    
    checkType(value);
    Object old = get();
    AVSConfig.config.put(name, value);
    boolean accept = changed.run(value, logger);
    
    // restore to old
    if (!accept) { 
      if (old.equals(defaultValue)) AVSConfig.config.remove(name);
      else AVSConfig.config.put(name, old);
    }
    
    return accept;
  }
  
  public String getDescription() { return getDescription(AVSConfig.logger); }
  /** Get description in the bundle using the name */
  public String getDescription(Logger logger) {
    return logger.getKey(bundleDescPrefix + name);
  }
  
  /** Only check the field name for equality */
  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || !(o instanceof ConfigField)) return false;
    return name.equals(((ConfigField) o).name);
  }
  
  @Override
  public String toString() {
    String v = ", value=";
    try { v += com.xpdustry.avs.util.Strings.objToStr(get()); }
    catch (IllegalStateException ignored) { v = ""; }
    return "{name=" + name + ", type=" + defaultValue.getClass() + v + "}";
  }
}
