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

import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.json.JsonWriterBuilder;
import com.xpdustry.avs.util.logging.Logger;


public class Field<T> {
  public final AConfig master;
  public final String name;
  public final Class<T> type;
  public final Class<?> elementType, keyType;
  
  protected final ChangeValider<T> validate;
  protected final T defaultValue;
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
    if (!loaded) load();
    return value;  
  }
  
  /** Can be overrides if a different way is used to put the value */
  protected void setValue(T value) {
    this.value = value;
    modified = loaded = true;
  }
  
  public synchronized T get() {
    // Allows getting values ​​while configuration is loading.
    if (!master.isLoading && !master.isLoaded()) 
      throw new IllegalStateException("master is not loaded");
    
    if (master.config == null) return defaultValue();
    return getValue();
  }
  
  public boolean set(T value) { return set(value, master.logger); }
  public synchronized boolean set(T value, Logger logger) {
    if (!master.isLoaded()) 
      throw new IllegalStateException("master is not loaded");
    
    T old = this.value;
    setValue(value);

    boolean accept = validateChange(this.value, logger);
    if (!accept) {
      this.value = old;
      modified = false;
    }
    return accept;
  }
  
  public T defaultValue() {
    return defaultValue;
  }

  public synchronized void load() {
    if (master.config.has(name)) value = master.config.get(name, type, elementType, keyType, defaultValue);
    else {
      value = defaultValue();
      master.config.put(name, elementType, keyType, defaultValue);
    }
    modified = false;
    loaded = true;
  }
  
  public synchronized void save() {
    if (modified) forcesave();
  }
  
  public synchronized void forcesave() {
    master.config.put(name, elementType, keyType, loaded ? value : defaultValue);
    modified = false;
  }

  public synchronized boolean setDefault(Logger logger) {
    T v = defaultValue();
    setValue(v);
    return validateChange(v, logger);
  }

  public synchronized boolean validateChange(Logger logger) {
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
    return toString(false);
  }
  
  public String toString(boolean formatted) {
    return toString(get(), formatted);
  }
  
  public String defaultToString() {
    return defaultToString(false);
  }
  
  public String defaultToString(boolean formatted) {
    return toString(defaultValue, formatted);
  }
  
  protected String toString(Object obj, boolean formatted) {
    if (formatted) {
      JsonWriterBuilder builder = new JsonWriterBuilder();
      master.config.getJson().setWriter(builder);
      master.config.getJson().writeValue(obj, type, elementType, keyType);
      return Strings.jsonPrettyPrint(builder.getJson(), arc.util.serialization.JsonWriter.OutputType.minimal);
    }
    
    return String.valueOf(master.config.getJson().toJson(obj, type, elementType, keyType));
  }
}