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

package com.xpdustry.avs.util.bundle;

import java.text.MessageFormat;
import java.util.Locale;

import com.xpdustry.avs.util.Strings;

import arc.struct.StringMap;
import arc.util.ArcRuntimeException;
import arc.util.TextFormatter;


/** Simple re-implementation of {@link arc.util.I18NBundle}. */
public class Bundle {
  public final StringMap properties = new StringMap();
  public final Locale locale;
  public final TextFormatter formatter;
  public Bundle parent;
  
  /** Build a new bundle for the specified {@code locale}. */
  public Bundle(Locale locale) {
    if (locale == null) throw new NullPointerException("locale cannot be null");
    this.locale = locale;
    this.formatter = new TextFormatter(locale, true);
  }
  
  /** 
   * Build a new bundle for the specified {@code locale} with {@code defaults} messages.
   * @apiNote {@code defaults} messages locales are not checked, it can be different as the specified locale.
   */
  public Bundle(Locale locale, StringMap defaults) {
    this(locale);
    properties.putAll(defaults);
  }
  
  /**
   * Clear current properties and load the new one from the specified file.
   * @throws ArcRuntimeException if the file is a directory, doesn't exist, could not be read,
   *                             reading error or a malformed properties.
   */
  public void load(arc.files.Fi file) throws ArcRuntimeException {
    java.io.Reader reader = null;
    
    try {
      reader = file.reader();
      properties.clear();
      arc.util.io.PropertiesUtils.load(properties, reader);  
      
    } catch (Exception e) {
      throw new ArcRuntimeException(e);
      
    } finally { arc.util.io.Streams.close(reader); }
  }
  
  /** 
   * Merge another bundle keys into this one.
   * @apiNote the locale must be the same and any existing values will be replaced.
   * @throws IllegalArgumentException if the locale is not the same as the other
   */
  public void merge(Bundle other) {
    if (!equals(other)) throw new IllegalArgumentException("bundles locales are not the same; " + 
                                                           other.locale +  " != " + locale);
    properties.putAll(other.properties);
  }
  
  /** Same as {@link #merge(Bundle)}, but doesn't replace existing keys. */
  public void mergeNoReplace(Bundle other) {
    if (!equals(other)) throw new IllegalArgumentException("bundles locales are not the same; " + 
                                                           other.locale +  " != " + locale);
    other.properties.each((k, v) -> {if (!properties.containsKey(k)) properties.put(k, v);});
  }
  
  /** @return whether this bundle has no keys */
  public boolean isEmpty() {
    return properties.isEmpty();
  }
  
  /**
   * Gets a string for the given key from this bundle or one of its parents.
   * @param key the key for the desired string
   * @return the string for the given key or the key surrounded by {@code ???} if it cannot be found
   */
  public String get(String key){
    String result = getOrNull(key);
    return result == null ? "???" + key + "???" : result;
  }
  
  /** @return the string for this given key, or def. */
  public String get(String key, String def){
    String result = getOrNull(key);
    return result == null ? def : result;
  }

  /** @return the string for this given key, or {@code null} */
  public String getOrNull(String key){
    String result = properties.get(key);
    if (result == null && parent != null) return parent.getOrNull(key);
    return result;
  }

  /**
   * Gets the string with the specified key from this bundle or one of its parent and format the given arguments.
   * @param key the key for the desired string
   * @param args the arguments to be replaced in the string associated to the given key.
   * @return the string for the given key formatted with the given arguments
   */
  public String format(String key, Object... args){
    return formatter.format(get(key), args);
  }
  
  /** Like {@link #format(String, Object...)}, but with a custom formatter. */
  public String format(MessageFormat formatter, String key, Object... args){
    return Strings.formatWithColor(formatter, get(key), "", "", args);
  }
  
  /** Like {@link #format(MessageFormat, String, Object...)} but with colors around placeholders. */
  public String formatColor(MessageFormat formatter, String key, String leftColor, String rightColor, Object... args) {
    return Strings.formatWithColor(formatter, get(key), leftColor, rightColor, args);
  }

  /** Checks whether a specified key is present in this bundle or in his parents. */
  public boolean has(String key){
    boolean result = properties.containsKey(key);
    if(!result && parent != null) return parent.has(key);
    return result;
  }
  
  /** 
   * This method protects against bundle recursion, by looking for itself (with identity) in the parents. 
   * @throws IllegalStateException if a bundle recursion is found in self's or other's parents
   */
  public void setParent(Bundle other) {
    // Iterate through self's and other's parents
    Bundle tmp1 = parent, tmp2 = other;
    String tmp3 = "", tmp4 = ""; //trace parents
    
    while (tmp1 != null || tmp2 != null) {
      if (tmp1 != null) tmp3 += (tmp3.isEmpty() ? "" : ", ") + tmp1.locale;
      if (tmp2 != null) tmp4 += (tmp4.isEmpty() ? "" : ", ") + tmp2.locale;
      
      if (tmp1 == this)
        throw new IllegalStateException("found a bundle recursion in self's parents. self=" + 
                                        locale + "; parents=" + tmp3);
      else if (tmp2 == this)
        throw new IllegalStateException("found a bundle recursion in other's parents. self=" + 
                                        locale + "; others=" + tmp4);
      
      if (tmp1 != null) tmp1 = tmp1.parent;
      if (tmp2 != null) tmp2 = tmp2.parent;
    }
    
    // If all okay, then set the parent
    parent = other;
  }

  /** @return the number of parent's bundle plus this bundle. */
  public int countParents() {
    int count = 1;
    if (parent != null) count += parent.countParents();
    return count;
  }

  /** Use locale as bundle equality */
  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || !(o instanceof Bundle)) return false;
    return ((Bundle) o).locale.equals(locale);
  }
}
