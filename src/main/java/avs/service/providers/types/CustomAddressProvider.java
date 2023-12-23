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

package avs.service.providers.types;

import avs.util.address.AddressValidity;


public abstract class CustomAddressProvider extends AddressProvider {
  public CustomAddressProvider(String name) { super(name); }
  public CustomAddressProvider(String displayName, String name) { super(displayName, name); }

  
  @Override
  public boolean load() {
    boolean loaded = super.load();
    
    if (!loaded) logger.err("Failed to load addresses! Skipping it...");
    else if (cache.isEmpty()) logger.warn("No addresses found.");
    else logger.info("Loaded @ addresses from cache file.", cache.size);
    
    return loaded;
  }
  
  public boolean blockAddress(AddressValidity address) {
    if (address == null) throw new NullPointerException("null address not allowed");
    // IP is valid, no need to add it in cache
    if (!address.type.isNotValid()) return false;
    // TODO: fire an event. Wanted?
    boolean added = cache.addUnique(address);
    if (added) save();
    return added;
  }

  public boolean allowAddress(AddressValidity address) {
    // TODO: fire an event. Wanted?
    boolean removed = cache.remove(address);
    if (removed) save();
    return removed;
  }
}
