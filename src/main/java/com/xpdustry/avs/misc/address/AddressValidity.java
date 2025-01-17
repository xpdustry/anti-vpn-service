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

import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.Strings;
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
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || !(obj instanceof AddressValidity)) return false;
    return subnet.equals(((AddressValidity) obj).subnet);
  }
  
  public String toFormattedString(Logger logger, boolean addLocation) {
    StringBuilder builder = new StringBuilder();
    toFormattedString(builder, logger, addLocation);
    return builder.toString();
  }
  
  public void toFormattedString(StringBuilder builder, Logger logger, boolean addLocation) {
    builder.append(logger.formatKey("avs.address-format.address", subnet)).append('\n');

    boolean[] types = Strings.int2bits(type.toBinary(), AddressType.numberOfTypes);
    Object[] args  = new Object[types.length];
    for (int i=0; i<args.length; i++) args[i] = types[i];
    builder.append(logger.formatKey("avs.address-format.security", args)).append('\n');
    
    if (infos != null) {
      if (addLocation) 
        builder.append(logger.formatKey("avs.address-format.location", 
            infos.location, infos.latitude, infos.longitude, infos.locale)).append('\n');
      builder.append(logger.formatKey("avs.address-format.infos", infos.ISP, infos.ASN)).append('\n');
    } else
      builder.append(logger.getKey("avs.address-format.no-more")).append('\n');
  }

  public static void checkAddress(String address) throws IllegalArgumentException {
    // single machine mask (or 0) is allowed
    int index = address.indexOf('/');
    if (index == -1) index = address.indexOf('%');
    if (index != -1) {
      int mask = arc.util.Strings.parseInt(address.substring(index+1), 0);
      address = address.substring(0, index);
      if (mask != 0 && mask != 32 && mask != 128)
        throw new IllegalArgumentException("Address must not have a mask, only subnets can.");
    }

    checkSubnet(address);
  }
  
  public static void checkSubnet(String subnet) throws IllegalArgumentException {
    if (!com.xpdustry.avs.util.network.InetAddressValidator.isValid(subnet)) 
      throw new IllegalArgumentException("Invalid or malformed address or subnet");
  }
}
