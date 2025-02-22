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

package com.xpdustry.avs.util.logging;

import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.bundle.*;

import arc.util.Log;
import arc.util.Log.LogLevel;


/** Log messages to console with topic and bundle support */
public class Logger {
  protected static final Object[] empty = {};
  
  public static String pluginTopic = "&lc[AVS]";
  public static String topicFormat = "&ly[@]";
  
  /** If {@code true} will not print the plugin and logger topics. */
  public boolean disableTopic = false;
  public final String topic;
  
  /** Will use slf4j when slf4md plugin is present */
  private static boolean slf4mdPresentAndEnabled = mindustry.Vars.mods.locateMod("slf4md") != null;
  private static Object slf4jLogger;
  
  /** Force log level, of all logger instances, to be always at debug */
  public static boolean forceDebug = false;
  
  public Logger() { 
    this((String)null); 
  }
  
  public Logger(boolean disableTopic) {
    this();
    this.disableTopic = disableTopic;
  }
  
  public Logger(Class<?> clazz) {
    this(clazz.getSimpleName());
  }
  
  public Logger(String topic) {
    if (slf4mdPresentAndEnabled && slf4jLogger == null) 
      slf4jLogger = org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    
    if (topic != null) {
      topic = topic.trim();
      if (!topic.isBlank()) {
        this.topic = Strings.format(topicFormat, topic);
        return;
      }
    }
    
    this.topic = null;
  }

  /** 
   * Return {@code true} if the log level is not enough to log the message. <br>
   * Can be used like that: {@code if (cannotLog(level)) return;}
   */
  public boolean cannotLog(LogLevel level) {
    return Log.level == LogLevel.none || (!forceDebug && Log.level.ordinal() > level.ordinal());
  }
  
  // region normal
  
  public void logNormal(LogLevel level, String text, Object... args) {
    if(cannotLog(level)) return;

    String tag = disableTopic ? "" : Log.format(pluginTopic+" "+(topic != null ? topic+"&fr " : "&fr"), empty);
    text = Log.format(text, args);
    
    if (!forceDebug && slf4mdPresentAndEnabled && slf4jLogger != null) {
      synchronized (slf4jLogger) {
        org.slf4j.Logger l = (org.slf4j.Logger) slf4jLogger;
        arc.func.Cons<String> printer;
        
        switch (level) {
          case debug: printer = l::debug; break;
          case info: printer = l::info; break;
          case warn: printer = l::warn; break;
          case err: printer = l::error; break;
          default: return;
        }

        for (String line : text.split("\n")) printer.get(tag + line);    
      }
      
    } else {
      synchronized (Log.logger) {
        for (String line : text.split("\n")) Log.logger.log(level, tag + line);   
      }
    }
  }
  
  public void logNormal(LogLevel level, String text) {
    logNormal(level, text, empty);
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
  
  /** Log an empty "info" line */
  public void none() {
    logNormal(LogLevel.info, "");
  }
  
  // endregion
  // region bundle
  
  ///// Followed methods can be overrides
  
  protected String getKeyImpl(String key) {
    return L10NBundle.get(key);
  }
  
  protected String formatKeyBundleImpl(Bundle bundle, String key, Object... args) {
    return bundle.formatColor(L10NBundle.getDefaultFormatter(), key, "&fb&lb", "&fr", args);
  }
  
  protected String formatKeyImpl(String key, Object... args) {
    return formatKeyBundleImpl(L10NBundle.getDefaultBundle(), key, args);
  }

  protected boolean hasKeyImpl(String key) {
    // Avoid recursion
    return L10NBundle.isDefaultBundleSet() ? L10NBundle.has(key) : false;
  }
  
  /////

  public String getKey(String key) {
    if (hasKeyImpl(key)) return getKeyImpl(key);
    else return RescueBundle.instance().get(key);
  }
  
  public String getKey(String key, String def) {
    if (hasKeyImpl(key)) return getKeyImpl(key);
    else return RescueBundle.instance().get(key, def);
  }
  
  public String formatKey(String key, Object... args) {
    if (hasKeyImpl(key)) return formatKeyImpl(key, args);
    else return formatKeyBundleImpl(RescueBundle.instance(), key, args);
  }
  
  public boolean hasKey(String key) {
    return L10NBundle.has(key);
  }
  
  public java.util.Locale getLocale() {
    return L10NBundle.getDefaultLocale();
  }
  
  public void log(LogLevel level, String key) {
    // avoid to search the key in the bundle, if the level is not enough to print the message
    if(cannotLog(level)) return;
    logNormal(level, getKey(key));
  }
 
  public void log(LogLevel level, String key, Object... args) {
    // avoid to search the key in the bundle, if the level is not enough to print the message
    if(cannotLog(level)) return;
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
  
  public void err(String key, Throwable th) {
    log(LogLevel.err, key, Strings.getStackTrace(th));
  }
  
  public void err(String key, Throwable th, Object... args) {
    log(LogLevel.err, key, arc.util.Structs.add(args, Strings.getStackTrace(th)));
  }
  
  // endregion
}
