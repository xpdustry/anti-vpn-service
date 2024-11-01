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

import com.xpdustry.avs.util.bundle.Bundle;
import com.xpdustry.avs.util.bundle.L10NBundle;

import arc.struct.StringMap;
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
    this(clazz.getSimpleName());
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
    String t = disableTopic ? "" : pluginTopic + " " + tag;
    for (String line : text.split("\n"))
      Log.log(level, t + line, args);
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
 
  /** Some keys, with important messages, are static, in case of default bundle is not loaded yet */
  private static final StringMap staticKeys = StringMap.of(
      "avs.general-error", "Error: {0}",
      "avs.config.msg.invalid-field", "{0} has an invalid value! Using the default one...", 
      "avs.loading.started", "Anti VPN Service (AVS) is loading...",
      "avs.loading.custom-bundles", "Loading custom bundles...",
      "avs.loading.bundle-loaded", "Loaded {0} locales, default is {1}.",
      "avs.loading.finished", "Loading finished in {0} seconds.",
      "avs.loading.failed", "An error occurred while loading the Anti VPN Service!",
      "avs.loading.security-error", "A security manager is present! Unable to take control of the 'ConnectPacket' listener.",
      "avs.loading.error", "Anti VPN service has been stopped and will not be used due to previous error!",
      "avs.loading.report", "You can report this error at: https://github.com/xpdustry/Anti-VPN-Service/issues/new",
      "avs.bundle.loading.started", "Loading bundles in directory: {0}",
      "avs.bundle.loading.list", "Appending {0} bundles.",
      "avs.bundle.loading.file.loaded", "Bundle loaded for locale {0}.",
      "avs.bundle.loading.file.error", "Error while loading bundle file: {0}",
      "avs.bundle.loading.hierarchy.aplying", "Applying bundles hierarchy...",
      "avs.bundle.loading.hierarchy.error", "Error while applying bundles hierarchy!",
      "avs.bundle.loading.done", "Loading done",
      "avs.bundle.warn.bundle-locale.msg1", "Bundles files with no language, in locale code, are not allowed.",
      "avs.bundle.warn.bundle-locale.msg2", "The file name should be: prefix_xx_XX.properties. Where xx and XX are the language and country.",
      "avs.bundle.warn.empty", "The bundle for locale '{0}' is empty.",
      "avs.bundle.not-found", "Unable to find a bundle for locale {0}. Using default one...",
      "avs.bundle.default-not-found", "Unable to a find bundle for the default locale '{0}'. Using machine local...",
      "avs.command.error-detected", "An error was detected during the Anti VPN service startup. Commands have been disabled."
  );
  private static final Bundle staticBundle = new Bundle(java.util.Locale.ENGLISH);
  
  static { 
    staticBundle.properties.putAll(staticKeys);
  }
  
  protected String getKey0(String key) {
    return L10NBundle.get(key);
  }
  
  protected String formatKeyBundle0(Bundle bundle, String key, Object... args) {
    return bundle.formatColor(L10NBundle.getDefaultFormatter(), key, "&fb&lb", "&fr", args);
  }
  
  protected String formatKey0(String key, Object... args) {
    return formatKeyBundle0(L10NBundle.getDefaultBundle(), key, args);
  }

  protected boolean hasKey0(String key) {
    return L10NBundle.has(key);
  }
  
  
  public String getKey(String key) {
    if (hasKey0(key)) return getKey0(key);
    else return staticBundle.get(key);
  }
  
  public String formatKey(String key, Object... args) {
    if (hasKey0(key)) return formatKey0(key, args);
    else return formatKeyBundle0(staticBundle, key, args);
  }
  
  public boolean hasKey(String key) {
    return hasKey0(key);
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
  
  public void err(String key, Throwable th) {
    log(LogLevel.err, key, Strings.getStackTrace(th));
  }
  
  public void err(String key, Throwable th, Object... args) {
    log(LogLevel.err, key, arc.util.Structs.add(args, Strings.getStackTrace(th)));
  }
  
  // endregion
}
