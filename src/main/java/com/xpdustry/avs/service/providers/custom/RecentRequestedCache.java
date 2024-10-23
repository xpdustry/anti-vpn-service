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

package com.xpdustry.avs.service.providers.custom;

import com.xpdustry.avs.misc.address.AddressValidity;


public class RecentRequestedCache extends com.xpdustry.avs.service.providers.type.EditableAddressProvider {
  //TODO: implement a timer to clear the cache frequently
  
  public RecentRequestedCache() {
    super("Recent Requested Cache", "recent-cache");
    // This provider is a cache for online services, so it's stored in the cache folder
    folder = com.xpdustry.avs.config.AVSConfig.cacheDirectory.get();
  }

  @Override
  protected boolean loadCache() {
    if (!super.loadCache()) return false;

    for (AddressValidity v : cache) {
      try { AddressValidity.checkIP(v.subnet.toString()); }
      catch (IllegalArgumentException e) { 
        logger.err("avs.provider.custom.recent-cache.load-failed", v.subnet);
        logger.err("avs.general-error", e.toString());
        cache.clear(); 
        return false;
      }
    } 
    return true;
  }
}
