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

import com.xpdustry.avs.util.network.Subnet;

import arc.struct.Seq;
import arc.util.serialization.JsonValue;


/** Oracle's public data-centers list. */
public class OracleCloud extends com.xpdustry.avs.service.providers.type.CloudDownloadedProvider {
  public OracleCloud() {
    super("oracle", "Oracle Cloud");
    url = "https://docs.cloud.oracle.com/en-us/iaas/tools/public_ip_ranges.json";
    providerType = ProviderType.dataCenter;
  }

  @Override
  protected Seq<Subnet> extractAddressRanges(JsonValue downloaded) {
    Seq<Subnet> list = new Seq<>();
    
    for (JsonValue values=downloaded.get("regions").child; values!=null; values=values.next) {
      for (JsonValue entry=values.get("cidrs").child; entry!=null; entry=entry.next) {
        list.add(Subnet.createInstance(entry.getString("cidr")));
      }
    }
    
    return list;
  }
}
