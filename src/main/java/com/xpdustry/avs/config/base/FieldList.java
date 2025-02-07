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

import arc.struct.Seq;


public class FieldList<T> extends CachedField<Seq<T>> {
  public final Class<?> elementType;

  public FieldList(AbstractConfig master, String name, Class<?> elementType, Seq<T> defaultValue) {
    this(master, name, elementType, defaultValue, null);
  }
  
  public FieldList(AbstractConfig master, String name, Class<?> elementType, Seq<T> defaultValue, 
                           ChangeValider<Seq<T>> validate) {
    super(master, name, defaultValue, validate);
    if (elementType == null) throw new NullPointerException(name() + ": elementType cannot be null");
    
    this.elementType = elementType;
  }

  @SuppressWarnings("unchecked")
  public void load() {
    cached = master.config.getJson(name(), Seq.class, elementType, this::defaultValue);
  }
  
  public void save() {
    master.config.putJson(name(), elementType, cached);
  }
  
  public boolean add(T value) { return add(value, master.logger); }
  public boolean add(T value, Logger logger) {
    Seq<T> v = get();
    if (!v.addUnique(value)) return false;
    
    boolean accept = validateChange(v, logger);
    if (!accept) v.pop();
    return accept;
  }
  
  public boolean remove(T value) { return remove(value, master.logger); }
  public boolean remove(T value, Logger logger) {
    Seq<T> v = get();
    int index = v.indexOf(value);
    if (index == -1) return false;
    v.remove(index);
    
    boolean accept = validateChange(v, logger);  
    if (!accept) v.insert(index, value);
    return accept;
  }
  
  @Override
  public String toString() {
    return master.config.getJson().toJson(cached);
  }
}
