/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2023 Xpdustry
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

package avs.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import arc.util.Log;
import arc.util.Log.LogLevel;


public class Logger{
  public static String pluginTopic = "&lc[Anti VPN Service]";
  
  public final String topic;
  private final Object[] empty = {};
  private final String tag;
  
  public Logger() { this(""); }
  public Logger(String topic) {
    this.topic = topic;
    this.tag = topic.isEmpty() ? "&fr" : topic + "&fr ";
  }
  
  public void log(LogLevel level, String text, Object... args){
    Log.log(level, pluginTopic + " " + tag + text, args);
  }

  public void debug(String text, Object... args){
    log(LogLevel.debug, text, args);
  }
  
  public void debug(Object object){
    debug(String.valueOf(object), empty);
  }
  
  public  void info(String text, Object... args){
    log(LogLevel.info, text, args);
  }
  
  public void info(Object object){
    info(String.valueOf(object), empty);
  }
  
  public void warn(String text, Object... args){
    log(LogLevel.warn, text, args);
  }
  
  public void err(String text, Object... args){
    log(LogLevel.err, text, args);
  }
  
  public void err(Throwable th){
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    th.printStackTrace(pw);
    err(sw.toString());
  }
  
  public void err(String text, Throwable th){
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    th.printStackTrace(pw);
    err(text + ": " + sw);
  }
}
