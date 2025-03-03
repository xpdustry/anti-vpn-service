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

import arc.struct.ObjectMap;


public class FieldMap<K, V> extends Field<ObjectMap<K, V>> {
  public FieldMap(AConfig master, String name, Class<?> keyType, Class<?> valueType, ObjectMap<K, V> defaultValue) {
    this(master, name, keyType, valueType, defaultValue, null);
  }
  
  public FieldMap(AConfig master, String name, Class<?> keyType, Class<?> valueType, ObjectMap<K, V> defaultValue, 
                  ChangeValider<ObjectMap<K, V>> validate) {
    super(master, name, Objects.requireNonNull(valueType), Objects.requireNonNull(keyType),
          defaultValue, validate);
  }

  @Override
  public ObjectMap<K, V> defaultValue() {
    return defaultValue.copy();
  }
  
  public synchronized V get(K key) {
    return get().get(key);
  }
  
  public boolean put(K key, V value) { return put(key, value, master.logger); }
  public synchronized boolean put(K key, V value, Logger logger) {
    ObjectMap<K, V> v = get();
    boolean exists = v.containsKey(key);
    V old = v.put(key, value);

    boolean accept = validateChange(v, logger);
    if (!accept) {
      if (exists) v.put(key, old);
      else v.remove(key);
    }
    return accept;
  }
  
  public boolean remove(K key) { return remove(key, master.logger); }
  public synchronized boolean remove(K key, Logger logger) {
    ObjectMap<K, V> v = get();
    if (!v.containsKey(key)) return false;
    V old = v.remove(key);
    
    boolean accept = validateChange(v, logger); 
    if (!accept) v.put(key, old);
    return accept;
  }
  
  public boolean contains(K key) {
    return get().containsKey(key);
  }
  
  public boolean contains(arc.func.Boolf2<K, V> predicate) {
    for (ObjectMap.Entry<K, V> e : get()) {
      if (predicate.get(e.key, e.value)) return true;
    }
    return false;
  }

  /** No need to validate change since the map will be clear */
  public void clear() {
    get().clear();
  }
}
