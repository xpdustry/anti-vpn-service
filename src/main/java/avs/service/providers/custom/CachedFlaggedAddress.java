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

package avs.service.providers.custom;


public class CachedFlaggedAddress extends avs.service.providers.types.CustomAddressProvider {
  public CachedFlaggedAddress() {
    super("Flagged Address Cache", avs.config.PVars.flaggedCacheProviderName);
  }

  @Override
  protected boolean loadCache() {
    if (!super.loadCache()) return false;
    
    // Little check because already flagged IPs are without mask, otherwise it would be a subnet.
    // So check if doen't have a mask and it's not a network IP or broadcast IP. 
    try { 
      cache.each(v -> {
        try { avs.util.address.AddressValidity.checkIP(v.ip.toString()); }
        catch (Exception e) { 
          logger.err("Failed to load flagged IPs cache file. (ip: @)", v.ip.toString());
          logger.err("Error: @", e.toString());
          // re-throw error to clear cache after
          throw e;
        }
      }); 
      
    } catch (Exception e) { 
      cache.clear(); 
      return false;
    }
    return true;
  }  
}
