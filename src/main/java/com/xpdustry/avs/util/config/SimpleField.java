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

package com.xpdustry.avs.util.config;

import com.xpdustry.avs.util.logging.Logger;


public class SimpleField extends Field<Object> {
  public SimpleField(AConfig master, String name, Object defaultValue) {
    super(master, name, defaultValue);
  }
  public SimpleField(AConfig master, String name, Object defaultValue, ChangeValider<Object> validate) {
    super(master, name, defaultValue, validate);
  }

  protected void checkType(Object o) {
    if (o == null) throw new NullPointerException(name + ": value to check is null");
    if (o.getClass() != type)
      throw new IllegalArgumentException(name + ": incompatible type " + o.getClass().getName() + 
                                         ". Must be a " + type.getName() + ".");
  }

  public boolean is(Class<?> clazz) {
    return type.isAssignableFrom(clazz);
  }
  
  public boolean isInt(){
    return defaultValue instanceof Integer;
  }
  
  public boolean isLong(){
    return defaultValue instanceof Long;
  }
  
  public boolean isFloat(){
    return defaultValue instanceof Float;
  }
  
  public boolean isDouble(){
    return defaultValue instanceof Double;
  }
  
  public boolean isBool(){
    return defaultValue instanceof Boolean;
  }
  
  public boolean isString(){
    return defaultValue instanceof String;
  }  

  /** To handle {@link #checkType(Object)} */
  @Override
  protected Object getValue() {
    Object o = super.getValue();
    checkType(o);
    return o;
  }
  
  public int getInt() {
    return (int)get();
  } 
  
  public long getLong() {
    return (long)get();
  } 
  
  public float getFloat() {
    return (float)get();
  }
  
  public double getDouble() {
    return (double)get();
  }
  
  public boolean getBool() {
    return (boolean)get();
  }
  
  public String getString() {
    return (String)get();
  }
  
  /** Override to handle {@link #checkType(Object)} */
  @Override
  public boolean set(Object value, Logger logger) {
    checkType(value);
    boolean accepted = super.set(value, logger);
    if (accepted) save(); // save directly the value
    return accepted;
  }
}
