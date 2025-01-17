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

import arc.struct.Seq;


public abstract class AbstractFieldList<T> extends AbstractField<Seq<T>> {
  public final Class<?> valueType;
  /** Cache the value to avoid coding/decoding it every times. */
  public Seq<T> values;

  public AbstractFieldList(AbstractConfig master, String name, Class<?> valueType, Seq<T> defaultValue) {
    this(master, name, valueType, defaultValue, null);
  }
  
  public AbstractFieldList(AbstractConfig master, String name, Class<?> valueType, Seq<T> defaultValue, 
                           ChangeValider<Seq<T>> validate) {
    super(master, name, defaultValue, validate);
    this.valueType = valueType;
  }

  @Override
  protected Seq<T> getValue() {
    if (values == null) load();
    return values;
  }
  
  @Override
  protected void putValue(Seq<T> value) {
    values = value;
  }
  
  @SuppressWarnings("unchecked")
  public void load() {
    values = master.config.getJson(name(), Seq.class, valueType, this::defaultValue);
    //changed = true;
  }
  
  public void save() {
    master.config.putJson(name(), valueType, values);
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
}
