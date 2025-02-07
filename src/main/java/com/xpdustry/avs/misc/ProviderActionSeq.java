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

import com.xpdustry.avs.config.RestrictedModeConfig;
import com.xpdustry.avs.service.providers.ProviderAction;

import arc.struct.Seq;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;


/** Just a class for serialization. Will be used by {@link RestrictedModeConfig} */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ProviderActionSeq extends Seq<ProviderAction> implements Json.Serializer<ProviderActionSeq> {
  public ProviderActionSeq() {}
  public ProviderActionSeq(Seq<ProviderAction> array){ super(array); }
  
  @Override
  public void write(Json json, ProviderActionSeq object, Class knownType) {
    json.writeValue(object.copy(), Seq.class, ProviderAction.class);
  }
  
  @Override
  public ProviderActionSeq read(Json json, JsonValue jsonData, Class type) {
    return new ProviderActionSeq(json.readValue(Seq.class, ProviderAction.class, jsonData));
  }
}
