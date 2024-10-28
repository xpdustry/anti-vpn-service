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

package com.xpdustry.avs.service.providers.type;

import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.misc.AVSEvents;
import com.xpdustry.avs.misc.address.AddressValidity;

import arc.Events;


public abstract class EditableAddressProvider extends CachedAddressProvider 
                                              implements ProviderCategories.Editable {
  public EditableAddressProvider(String displayName) { 
    super(displayName); 
    // Custom providers are saved in the settings directory
    folder = AVSConfig.settingsDirectory.get();
  }
  
  public EditableAddressProvider(String displayName, String name) { 
    super(displayName, name); 
    folder = AVSConfig.settingsDirectory.get();
  }

  @Override
  public boolean add(AddressValidity address) {
    if (address == null) throw new NullPointerException("null address not allowed");

    boolean added = cache.addUnique(address);
    if (added) {
      save();
      Events.fire(new AVSEvents.EditableProviderAddedAddressEvent(this, address, added));
    }
    return added;
  }

  @Override
  public boolean remove(AddressValidity address) {
    boolean removed = cache.remove(address);
    if (removed) {
      save();
      Events.fire(new AVSEvents.EditableProviderRemovedAddressEvent(this, address, removed));
    }
    return removed;
  }
  
  @Override
  public void clear() {
    Events.fire(new AVSEvents.EditableProviderCleaningAddressesEvent(this));
    cache.clear();
    save();
    logger.info("avs.provider.editable.cleaned");
  }
}
