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

package com.xpdustry.avs.misc.address;

import com.xpdustry.avs.util.network.Subnet;


public class AddressValidity {
  public final Subnet subnet;
  public final @arc.util.Nullable AddressInfos infos;
  public AddressStats stats = new AddressStats();
  public AddressType type = new AddressType();

  public AddressValidity(String subnet) { this(subnet, null); }
  public AddressValidity(String subnet, AddressInfos infos) { this(Subnet.createInstance(subnet), infos); }
  public AddressValidity(Subnet subnet) { this(subnet, null); }
  public AddressValidity(Subnet subnet, AddressInfos infos) {
    this.subnet = subnet;
    this.infos = infos;
  }
  
  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof AddressValidity)) return false;
    final AddressValidity other = (AddressValidity) obj;
    return subnet.equals(other.subnet);
  }

  public static void checkIP(String ip) {
    try {
      // single machine mask is allowed
      int index = ip.indexOf('/');
      if (index == -1) index = ip.indexOf('%');
      if (index != -1) {
        int mask = arc.util.Strings.parseInt(ip.substring(index+1), 0);
        ip = ip.substring(0, index);
        if (mask != 0 && mask != 32)
          throw new IllegalArgumentException("IP address must not have a mask, only subnet addresses can. (ip: " + ip + ")");
      }
        
      // Convert the ip to InetAddress, to check its validity.
      // Because we cannot access the class InetAddressUtil class,
      // used to check formatting of address
      
      if (java.net.InetAddress.getByName(ip).isLoopbackAddress()) {
        throw new IllegalArgumentException("Loopback address is not allowed. (ip: " + ip + ")");
      }
      
    } catch (java.net.UnknownHostException e) { 
      throw new IllegalArgumentException(e.getLocalizedMessage() + ". (ip: " + ip + ")"); 
    }
  }
}
