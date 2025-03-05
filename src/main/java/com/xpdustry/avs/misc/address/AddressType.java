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

package com.xpdustry.avs.misc.address;


public class AddressType {
  // Number of address types, this facilitate the coding of boolean values
  public static final int numberOfTypes = 6;
  
  public boolean other = false,
      vpn = false, 
      proxy = false, 
      tor = false, 
      relay = false,
      dataCenter = false;
  
  public boolean isNotValid() {
    return other || vpn || proxy || tor || relay || dataCenter;
  }
  
  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof AddressType)) return false;
    final AddressType type = (AddressType) obj;
    return vpn == type.vpn &&  proxy == type.proxy &&
           tor == type.tor && relay == type.relay &&
           dataCenter == type.dataCenter && other == type.other;
  }
}
