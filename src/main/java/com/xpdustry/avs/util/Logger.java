/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024 Xpdustry
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

package com.xpdustry.avs.util;

import com.xpdustry.avs.util.bundle.L10NBundle;

import arc.util.Log;
import arc.util.Log.LogLevel;


/** Log messages to console with topic and bundle support */
public class Logger{
  protected static final Object[] empty = {};
  
  public static String pluginTopic = "&lc[AVS]";
  /** The formatter for logger topics. Only used when creating a new instance of {@link Logger}. */
  public static String topicFormat = "&ly[@]&fr";
  
  /** If {@code true} will not print the plugin and logger topic. */
  public boolean disableTopic = false;
  public final String topic;
  protected final String tag;
  
  
  public Logger() { 
    this(""); 
  }
  
  public Logger(boolean disableTopic) {
    this("");
    this.disableTopic = disableTopic;
  }
  
  public Logger(Class<?> clazz) {
    this(clazz.getName());
  }
  
  public Logger(String topic) {
    this.topic = topic;
    this.tag = topic.isEmpty() ? "&fr" : Strings.format(topicFormat, topic) + " ";
  }

  // region normal
  
  public void logNormal(LogLevel level, String text) {
    logNormal(level, text, empty);
  }
  
  public void logNormal(LogLevel level, String text, Object... args) {
    Log.log(level, (disableTopic ? "" : pluginTopic + " " + tag) + text, args);
  }

  public void debugNormal(String text, Object... args) {
    logNormal(LogLevel.debug, text, args);
  }

  public void debugNormal(Object object) {
    debugNormal(String.valueOf(object), empty);
  }

  public void infoNormal(String text, Object... args) {
    logNormal(LogLevel.info, text, args);
  }
  
  public void infoNormal(Object object) {
    infoNormal(String.valueOf(object), empty);
  }

  public void warnNormal(String text, Object... args) {
    logNormal(LogLevel.warn, text, args);
  }
  
  public void warnNormal(String text) {
    warnNormal(text, empty);
  }  
  
  public void errNormal(String text, Object... args) {
    logNormal(LogLevel.err, text, args);
  }
  
  public void errNormal(String text) {
    errNormal(text, empty);
  }
  
  public void errNormal(String text, Throwable th) {
    errNormal(text + ": " + Strings.getStackTrace(th));
  }
  
  public void err(Throwable th){
    errNormal(Strings.getStackTrace(th));
  } 
  
  // endregion
  // region bundle
  
  public String getKey(String key) {
    return L10NBundle.get(key);
  }
  
  public String formatKey(String key, Object... args) {
    return L10NBundle.formatColor(key, args);
  }
  
  public void log(LogLevel level, String key) {
    // avoid to search the key in the bundle, if the level is not enough to print the message
    if(Log.level.ordinal() > level.ordinal()) return;
    logNormal(level, getKey(key));
  }
 
  public void log(LogLevel level, String key, Object... args) {
    if(Log.level.ordinal() > level.ordinal()) return;
    logNormal(level, formatKey(key, args));
  }
  
  public void debug(String key) {
    log(LogLevel.debug, key);
  } 
  
  public void debug(String key, Object... args) {
    log(LogLevel.debug, key, args);
  }
  
  public void info(String key) {
    log(LogLevel.info, key);
  }
  
  public void info(String key, Object... args) {
    log(LogLevel.info, key, args);
  }
  
  public void warn(String key) {
    log(LogLevel.warn, key);
  }
  
  public void warn(String key, Object... args) {
    log(LogLevel.warn, key, args);
  }
  
  public void err(String key) {
    log(LogLevel.err, key);
  }
  
  public void err(String key, Object... args) {
    log(LogLevel.err, key, args);
  }
  
  // Special case for this method, we getting the key before logging
  public void err(String key, Throwable th) {
    logNormal(LogLevel.err, getKey(key) + ": " + Strings.getStackTrace(th));
  }
  
  public void err(String key, Throwable th, Object... args) {
    logNormal(LogLevel.err, formatKey(key, args) + ": " + Strings.getStackTrace(th));
  }
  
  // endregion
}
