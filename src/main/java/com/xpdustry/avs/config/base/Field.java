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

import com.xpdustry.avs.util.logging.Logger;


public class Field<T> implements IField<T> {
  public final String name;
  public final T defaultValue;
  protected final ChangeValider<T> validate;
  protected final AbstractConfig master;

  public Field(AbstractConfig master, String name, T defaultValue) {
    this(master, name, defaultValue, null); 
  }
  public Field(AbstractConfig master, String name, T defaultValue, ChangeValider<T> validate) {
    if (name == null || name.isEmpty()) throw new NullPointerException(name() + ": name cannot be null");
    if (defaultValue == null) throw new NullPointerException(name() + ": defaultValue cannot be null");
    
    this.name = name;
    this.defaultValue = defaultValue;
    this.validate = validate;
    this.master = master;
    
    master.all.add(this);
  }
  
  @Override
  public String name() {
    return name;
  }
  
  @Override
  public T defaultValue() {
    return defaultValue;
  }
  
  @Override
  public String desc(Logger logger) {
    return logger.getKey(arc.util.Strings.format(master.fieldDescBundleKey(this), name()));
  }
  
  @Override
  public T get() {
    // Allows getting values ​​if configuration is loading.
    if (!master.isLoading && !master.isLoaded()) 
      throw new IllegalStateException("master config is not loaded");
    
    if (master.config == null) return (T) defaultValue();
    return getValue();
  }

  @Override
  public boolean set(T value) { return set(value, master.logger); }
  public boolean set(T value, Logger logger) {
    if (!master.isLoaded()) 
      throw new IllegalStateException("master config is not loaded");
    
    boolean accept = validateChange(value, logger);
    if (accept) putValue(value);
    return accept;
  }
  
  /** Can be overrides if a different way is used to get the value */
  @SuppressWarnings("unchecked")
  protected T getValue() {
    T defaults = defaultValue();
    return (T) (master.config.isBasicType(defaults) ? master.config.getOrPut(name(), defaults) :
                master.config.getJson(name(), (Class<T>) defaults.getClass(), () -> defaults));
  }
  
  /** Can be overrides if a different way is used to put the value */
  protected void putValue(T value) {
    if (master.config.isBasicType(value))
         master.config.put(name(), value);
    else master.config.putJson(name(), value);
  }
  
  @Override
  public void setDefault(Logger logger) {
    putValue(defaultValue());
    validateChange(defaultValue(), logger);
  }
  
  @Override
  public boolean validateChange(Logger logger) {
    return validateChange(get(), logger);
  }
  
  protected boolean validateChange(T newValue, Logger logger) {
    return validate == null || validate.accept(newValue, logger);
  }
  
  
  /** Only the field name is used as equality */
  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || !(o instanceof Field)) return false;
    return name().equals(((Field) o).name());
  }
  
  /** Return the value converted to string */
  @Override
  public String toString() {
    return String.valueOf(get());
  }
}
