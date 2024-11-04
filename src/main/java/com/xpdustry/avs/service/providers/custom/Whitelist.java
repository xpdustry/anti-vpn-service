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

import arc.util.serialization.Json;


public class Whitelist extends com.xpdustry.avs.service.providers.type.EditableAddressProvider {
  public Whitelist() {
    super("whitelist", "Whitelist", false);
  }
  
  /** 
   * Override the {@link AddressValidity} serializer, 
   * because we only need the address for the whitelist
   */
  @SuppressWarnings("rawtypes")
  @Override
  protected boolean loadMiscSettings() {
    Json json = new Json();
    com.xpdustry.avs.misc.JsonSerializer.apply(json);
    json.setSerializer(AddressValidity.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, AddressValidity object, Class knownType) {
        json.writeValue(object.subnet.toString());
      }
      @Override
      public AddressValidity read(Json json, arc.util.serialization.JsonValue jsonData, Class type) {
        return new AddressValidity(jsonData.asString());
      }
    });
    super.getSettings().setJson(json);
    
    return super.loadMiscSettings();
  }
}
