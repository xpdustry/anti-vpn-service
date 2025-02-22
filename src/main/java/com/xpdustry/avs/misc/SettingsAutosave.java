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

package com.xpdustry.avs.misc;

import com.xpdustry.avs.util.json.JsonSettings;
import com.xpdustry.avs.util.logging.Logger;

import arc.struct.Seq;


public class SettingsAutosave {
  protected final static Seq<JsonSettings> all = new Seq<>();
  protected final static Logger logger = new Logger("Autosave");
  
  protected static int spacing = 360; // in seconds
  protected static Thread thread;
  protected static String threadName = "Autosave";

  public static void add(JsonSettings settings) {
    all.add(settings);
  }
  
  public synchronized static boolean runNeeded() {
    return all.contains(JsonSettings::modified);
  }
  
  public synchronized static void run() {
    // Only save if changes are made
    if (runNeeded()) {
      logger.info("avs.autosave.started");
      for (JsonSettings s : all) {
        try { s.save(); } 
        catch (RuntimeException e) {
          logger.err("avs.autosave.failed", s.file());
          logger.err("avs.general-error", e);
          return;
        }
      }
      logger.info("avs.autosave.finished");      
    }
  }
  
  public static boolean start() { return start(null); }
  public static boolean start(String threadName) {
    if (thread == null) {
      if (threadName == null || threadName.isBlank())
           threadName = SettingsAutosave.threadName;
      else SettingsAutosave.threadName = threadName;
        
      thread = arc.util.Threads.daemon(threadName, () -> {
        logger.info("avs.autosave.thread.started");
        
        while (true) {
          try { Thread.sleep(spacing * 1000); } 
          catch (InterruptedException e) { 
            logger.info("avs.autosave.thread.stopped");
            return; 
          }; 
          
          run(); 
        }
      });
      return true;
    }
    return false;
  }
  
  public static boolean stop() {
    if (thread != null) {
      thread.interrupt();
      try { thread.join(1000);  } 
      catch (InterruptedException ignored) {}
      thread = null;
      return true;
    }
    return false;
  }

  
  public static boolean isRunning() {
    return thread != null && thread.isAlive();
  }
  
  public static int spacing() {
    return spacing;
  }
  
  public static void spacing(int spacing) {
    if (spacing < 1) throw new IllegalArgumentException("spacing must be greater than 1 second");
    SettingsAutosave.spacing = spacing;
  }
}
