/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2025 Xpdustry
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

import java.util.Objects;

import com.xpdustry.avs.util.logging.Logger;


public class Field<T> {
  public final AConfig master;
  public final String name;
  public final T defaultValue;
  public final Class<T> type;
  public final Class<?> elementType, keyType;
  
  protected final ChangeValider<T> validate;
  protected boolean modified, loaded;
  protected T value;
  
  public Field(AConfig master, String name, T def) {
    this(master, name, null, def);
  }
  
  public Field(AConfig master, String name, T def, ChangeValider<T> validate) {
    this(master, name, null, def, validate);
  }
  
  public Field(AConfig master, String name, Class<?> elementType, T def) { 
    this(master, name, elementType, null, def); 
  }
  
  public Field(AConfig master, String name, Class<?> elementType, T def, ChangeValider<T> validate) { 
    this(master, name, elementType, null, def, validate); 
  }
  
  public Field(AConfig master, String name, Class<?> elementType, Class<?> keyType, T def) { 
    this(master, name, elementType, keyType, def, null);
  }

  @SuppressWarnings("unchecked")
  public Field(AConfig master, String name, Class<?> elementType, Class<?> keyType, T def, ChangeValider<T> validate) {
    this.master = Objects.requireNonNull(master);
    this.name = Objects.requireNonNull(name);
    this.defaultValue = Objects.requireNonNull(def);
    this.type = (Class<T>) def.getClass();
    this.keyType = keyType;
    this.elementType = elementType;
    this.validate = validate;

    master.all.add(this);
  }
  
  public String desc(Logger logger) {
    return logger.getKey(master.getFieldDescKey(this));
  }

  public boolean modified() {
    return modified;
  }
  
  /** Can be overrides if a different way is used to get the value */
  protected T getValue() {
    if (!loaded) {
      load();
      loaded = true;
    }
    return value;  
  }
  
  /** Can be overrides if a different way is used to put the value */
  protected void setValue(T value) {
    this.value = value;
    modified = loaded = true;
  }
  
  public T get() {
    // Allows getting values ​​while configuration is loading.
    if (!master.isLoading && !master.isLoaded()) 
      throw new IllegalStateException("master is not loaded");
    
    if (master.config == null) return defaultValue;
    return getValue();
  }
  
  public boolean set(T value) { return set(value, master.logger); }
  public boolean set(T value, Logger logger) {
    if (!master.isLoaded()) 
      throw new IllegalStateException("master is not loaded");
    
    boolean accept = validateChange(value, logger);
    if (accept) setValue(value);
    return accept;
  }

  public void load() {
    value = master.config.getOrPut(name, type, elementType, keyType, defaultValue);
    modified = false;
  }
  
  public void save() {
    if (modified) forcesave();
  }
  
  public void forcesave() {
    master.config.put(name, elementType, keyType, loaded ? value : defaultValue);
    modified = false;
  }

  public void setDefault(Logger logger) {
    setValue(defaultValue);
    validateChange(defaultValue, logger);
  }

  public boolean validateChange(Logger logger) {
    return validateChange(get(), logger);
  }
  
  protected boolean validateChange(T newValue, Logger logger) {
    return validate == null || validate.accept(newValue, logger);
  }
  
  
  /** Only the field name is used as equality */
  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || !(o instanceof Field)) return false;
    return name.equals(((Field<?>) o).name);
  }
  
  /** Return the value converted to string */
  @Override
  public String toString() {
    return String.valueOf(master.config.getJson().toJson(get()));
  }
}