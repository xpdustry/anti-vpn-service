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

package com.xpdustry.avs.service.providers.local;

import com.xpdustry.avs.service.providers.type.ProviderType;
import com.xpdustry.avs.util.network.Subnet;

import arc.struct.Seq;
import arc.util.serialization.JsonValue;


/** Google's public data-centers, proxies and crawlers list. */
public class GoogleCloud extends com.xpdustry.avs.service.providers.type.CloudDownloadedProvider {
  public GoogleCloud() {
    super("google", "Google");
    url = "https://www.gstatic.com/ipranges/cloud.json";
    providerType = ProviderType.proxy;
  }

  @Override
  protected Seq<Subnet> extractAddressRanges(JsonValue downloaded) {
    Seq<Subnet> list = new Seq<>();
    int i = 0;
    
    for (JsonValue values=downloaded.get("prefixes").child; values!=null; values=values.next, i++) {
      JsonValue prefix = values.get("ipv4Prefix");
      if (prefix == null) prefix = values.get("ipv6Prefix");
      
      if (prefix == null) logger.debug("avs.provider.cloud.google.find-address-failed", i);
      else list.add(Subnet.createInstance(prefix.asString()));
    }
    
    return list;
  }
}
