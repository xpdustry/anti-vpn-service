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

import com.xpdustry.avs.util.Logger;


public abstract class AbstractField<T> {
  public final String name;
  public final T defaultValue;
  protected final ChangeValider<T> validate;
  protected final AbstractConfig master;
  
  public AbstractField(AbstractConfig master, String name, T defaultValue) {
    this(master, name, defaultValue, null); 
  }
  public AbstractField(AbstractConfig master, String name, T defaultValue, ChangeValider<T> validate) {
    this.name = name;
    if (defaultValue == null) throw new NullPointerException(name + ": defaultValue cannot be null");
    this.defaultValue = defaultValue;
    this.validate = validate;
    this.master = master;
    
    master.all.add(this);
  }
  
  public String name() {
    return name;
  }
  
  public T defaultValue() {
    return defaultValue;
  }
  
  public String desc(Logger logger) {
    return logger.getKey(arc.util.Strings.format(descKeyFormat(), name()));
  }
  
  protected abstract String descKeyFormat();
  
  public T get() {
    // Allows getting values ​​if configuration is loading.
    if (!master.isLoading && !master.isLoaded()) 
      throw new IllegalStateException("master config is not loaded");
    
    if (master.config == null) return (T) defaultValue();
    return getValue();
  }
  
  /** Override this if a different way is used to get the value */
  @SuppressWarnings("unchecked")
  protected T getValue() {
    return (T) master.config.getOrPut(name(), defaultValue());
  }
  
  public boolean set(T value) { return set(value, master.logger); }
  public boolean set(T value, Logger logger) {
    if (!master.isLoaded()) 
      throw new IllegalStateException("master config is not loaded");
    
    T old = get();
    putValue(value);
    
    boolean accept = validateChange(value, logger);
    if (!accept) putValue(old); // restore to old
    return accept;
  }
  
  /** Override this if a different way is used to put the value */
  protected void putValue(T value) {
    master.config.put(name(), value);
  }
  
  protected void setDefault(Logger logger) {
    putValue(defaultValue());
    validateChange(defaultValue(), logger);
  }
  
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
    if (o == null || !(o instanceof AbstractField)) return false;
    return name().equals(((AbstractField) o).name());
  }
  
  /** Return the value converted to string */
  @Override
  public String toString() {
    return String.valueOf(get());
  }
}
