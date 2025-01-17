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

import arc.func.Func;
import arc.struct.Seq;


/** Same as {@link AbstractFieldList} but with predefined string serializer/deserializer */
public abstract class AbstractFieldListSerialized<T> extends AbstractFieldList<T> {
  protected final Func<String, T> loader;
  protected final Func<T, String> saver;
  
  public AbstractFieldListSerialized(AbstractConfig master, String name, Seq<T> defaultValue,
                                     Func<String, T> loader, Func<T, String> saver) {
    super(master, name, String.class, defaultValue);
    this.loader = loader;
    this.saver = saver;
  }

  public AbstractFieldListSerialized(AbstractConfig master, String name, Seq<T> defaultValue,
                                     ChangeValider<Seq<T>> validate, 
                                     Func<String, T> loader, Func<T, String> saver) {
    super(master, name, String.class, defaultValue, validate);
    this.loader = loader;
    this.saver = saver;
  }
  
  @SuppressWarnings("unchecked")
  public void load() {
    values = master.config.getJson(name(), Seq.class, valueType, () -> defaultValue().map(saver))
                          .map(loader);
    //changed = true;
  }
  
  public void save() {
    master.config.putJson(name(), valueType, values.map(saver));
  }

  @Override
  public String toString() {
    return '[' + get().toString(", ", saver) + ']';
  }
}
