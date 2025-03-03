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

import com.xpdustry.avs.util.json.JsonWriterBuilder;
import com.xpdustry.avs.util.logging.Logger;

import arc.struct.ObjectMap;
import arc.struct.Seq;


public class FieldMapSeq<K, V> extends FieldMap<K, Seq<V>> {
  public final Class<V> seqElementType;
  
  public FieldMapSeq(AConfig master, String name, Class<?> keyType, Class<V> valueType, 
                     ObjectMap<K, Seq<V>> defaultValue) {
    this(master, name, keyType, valueType, defaultValue, null);
  }
  
  public FieldMapSeq(AConfig master, String name, Class<?> keyType, Class<V> valueType, 
                     ObjectMap<K, Seq<V>> defaultValue, ChangeValider<ObjectMap<K, Seq<V>>> validate) {
    super(master, name, Objects.requireNonNull(keyType), Seq.class, defaultValue, validate);
    seqElementType = Objects.requireNonNull(valueType);
  }
  
  @Override
  public ObjectMap<K, Seq<V>> defaultValue() {
    ObjectMap<K, Seq<V>> out = new ObjectMap<>(defaultValue.size);
    for (ObjectMap.Entry<K, Seq<V>> e : defaultValue) 
      out.put(e.key, e.value.copy());
    return out;
  }
  
  /** Read is more difficult because of the the third inferred type, only two are supported (key and value). */
  @Override
  public synchronized void load() {
    if (!master.config.has(name)) master.config.put(name, elementType, keyType, value = defaultValue());
    else {
      // Value is not a V type. We need a second de-serialization
      ObjectMap<K,Seq<V>> decoded = master.config.get(name, type, elementType, keyType, defaultValue);
      JsonWriterBuilder builder = new JsonWriterBuilder();
      
      try {
        for (ObjectMap.Entry<K, Seq<V>> e : decoded) {
          for (int i=0; i<e.value.size; i++) {
            builder.value(e.value.get(i));
            e.value.set(i, master.config.getJson().readValue(seqElementType, builder.getJson()));
            builder.clear();
          }
        }
        builder.close();
      } catch (java.io.IOException e) { //never throw an IOException
        throw new arc.util.serialization.SerializationException(e);
      }
      
      value = decoded;
    }
    modified = false;
    loaded = true;
  }
  
  public boolean add(K key, V value) { return add(key, value, master.logger); }
  public synchronized boolean add(K key, V value, Logger logger) {
    ObjectMap<K,Seq<V>> v = get();
    Seq<V> items;
    
    // if doesn't exists, add a new key
    boolean exists = v.containsKey(key);
    if (!exists) v.put(key, items = new Seq<>());
    else items = v.get(key);
    if (!items.addUnique(value)) return false;
    
    boolean accept = validateChange(v, logger);
    if (!accept) {
      if (!exists) v.remove(key);
      else items.pop();
    }
    return accept;
  }
  
  public boolean remove(K key, V value) { return remove(key, value, master.logger); }
  public synchronized boolean remove(K key, V value, Logger logger) {
    ObjectMap<K,Seq<V>> v = get();
    if (!v.containsKey(key)) return false;
    Seq<V> items = v.get(key);
    int index = items.indexOf(value);
    if (index == -1) return false;
    
    // If one item left, remove the key directly
    boolean oneLeft = items.size == 1;
    if (oneLeft) v.remove(key);
    else items.remove(index);
    
    boolean accept = validateChange(v, logger);  
    if (!accept) {
      if (oneLeft) v.put(key, items);
      else items.insert(index, value);
    }
    return accept;
  }

  public boolean contains(K key, V value) {
    if (!contains(key)) return false;
    return get(key).contains(value);
  }
  
  public boolean contains(K key, arc.func.Boolf<V> predicate) {
    if (!contains(key)) return false;
    return get(key).contains(predicate);
  }
  
  /** @return {@code false} if the key doesn't exist */
  public boolean clear(K key) {
    ObjectMap<K,Seq<V>> v = get();
    if (!v.containsKey(key)) return false;
    v.get(key).clear();
    return true;
  }
}
