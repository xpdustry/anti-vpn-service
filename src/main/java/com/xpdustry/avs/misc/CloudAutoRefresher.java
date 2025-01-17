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

package com.xpdustry.avs.misc;

import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.providers.type.CloudDownloadedProvider;
import com.xpdustry.avs.service.providers.type.ProviderCategories;
import com.xpdustry.avs.util.Logger;

import arc.Events;


/** 
 * Auto refresher for {@link CloudDownloadedProvider} using {@link AntiVpnService#allProviders}
 * and sorted by the {@link ProviderCategories#Cloudable} interface
 */
public class CloudAutoRefresher {
  private static Logger logger = new Logger(CloudAutoRefresher.class);
  private static Thread thread;
  private static long spacing = 360 * 60 * 1000; // in ms, default is every 6 hours
  
  /** Refresh providers identified by the {@link ProviderCategories#Cloudable} interface */
  public static void refreshProviders() {
    logger.none();
    logger.info("avs.refresher.trigger");
    logger.none();
    Events.fire(new AVSEvents.CloudAutoRefresherStartedEvent());
    boolean error = false;
    
    for (int i=0; i<AntiVpnService.allProviders.size; i++) {
      if (AntiVpnService.allProviders.get(i) instanceof ProviderCategories.Cloudable) {
        if (!((ProviderCategories.Cloudable)AntiVpnService.allProviders.get(i)).refresh())
          error = true;
      }
    }
    
    logger.none();
    if (error) {
      Events.fire(new AVSEvents.CloudAutoRefresherFinishedEvent());
      logger.warn("avs.refresher.done-errors");
    } else {
      Events.fire(new AVSEvents.CloudAutoRefresherFinishedWithErrorsEvent());
      logger.info("avs.refresher.done");
    }
    logger.none();
  }
  
  
  public static boolean start() {
    if (thread == null && spacing > 0) {
      thread = arc.util.Threads.daemon(logger.topic, () -> {
        logger.info("avs.refresher.started");

        while (true) {
          try { Thread.sleep(spacing); } 
          catch (InterruptedException e) { 
            logger.info("avs.refresher.stopped");
            return; 
          }; 
          
          refreshProviders(); 
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
  
  public static boolean running() {
    return thread != null && thread.isAlive();
  }
  
  public static int spacing() {
    return (int) (spacing / 1000 / 60);
  }
  
  public static void spacing(int minutes) {
    if (minutes < 1) throw new IllegalArgumentException("spacing must be greater than 1 minute");
    logger.info("avs.refresher.spacing", minutes);
    spacing = minutes * 60 * 1000;
  }
}
