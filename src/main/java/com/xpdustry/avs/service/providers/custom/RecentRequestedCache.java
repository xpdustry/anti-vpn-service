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

package com.xpdustry.avs.service.providers.custom;

import com.xpdustry.avs.misc.address.AddressValidity;

import arc.util.Timer;


/** Cache provider used by online providers, to store the recently requested addresses' informations. */
public class RecentRequestedCache extends com.xpdustry.avs.service.providers.type.EditableAddressProvider 
       implements com.xpdustry.avs.service.providers.type.ProviderCategories.Cleanable {
  private final Timer.Task cleanupTimer = new Timer.Task() {
      @Override
      public void run() {
        if (cacheSize() > 0) {
          logger.info("avs.provider.editable.recent-cache.cleanup");
          clear();
        }
      }
    };
  
  public RecentRequestedCache() {
    super("recent-cache", "Recent Requested Cache", true);
  }

  @Override
  protected boolean loadMiscSettings() {
    if (!super.loadMiscSettings()) return false;
    
    for (AddressValidity v : cache) {
      try { AddressValidity.checkAddress(v.subnet.toString()); }
      catch (IllegalArgumentException e) { 
        logger.err("avs.provider.editable.recent-cache.load-failed", v.subnet);
        logger.err("avs.general-error", e.toString());
        cache.clear(); 
        return false;
      }
    }      
 
    return true;
  }
  
  @Override
  public void scheduleCleanup(float spacing) {
    if (spacing < 0) throw new IllegalArgumentException("invalid spacing. must be 0 or greater");
    else if (spacing == 0) {
      logger.info("avs.provider.editable.recent-cache.cleanup.disabled");
      cleanupTimer.cancel();
    } else {
      logger.info("avs.provider.editable.recent-cache.cleanup.scheduled", 
          com.xpdustry.avs.util.Strings.duration2str(logger, (long) (spacing*60*1000), false, 2));
      if (cleanupTimer.isScheduled()) cleanupTimer.cancel();
      Timer.schedule(cleanupTimer, spacing * 60, spacing * 60);
    }
  }
}
