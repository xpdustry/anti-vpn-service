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

package avs.service.providers.online;

import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;

import avs.util.address.*;
import avs.util.Strings;


public class VpnApiService extends avs.service.providers.types.OnlineServiceAddressProvider {
  public VpnApiService() {
    super("VpnAPI.io Service", "vpnapi", "https://vpnapi.io/api/{0}?key={1}");
    canUseTokens = true;
    urlWithoutTokens = "https://vpnapi.io/api/{0}";
    isTrusted = true;
  }

  @Override
  public void handleReply(String content, ServiceReply reply) throws Exception {
    JsonValue soup = new JsonReader().parse(content);
    
    if (soup.size == 0) {
      reply.type = ServiceReplyType.ERROR;
      return;
      
    } else if (soup.has("message")) {
      reply.message = soup.getString("message");
      String message = reply.message.toLowerCase();
      
      if (message.contains("invalid api key")) {
        reply.type = ServiceReplyType.INVALID_TOKEN;
        return;
        
      } else if (message.contains("is not a valid ip") ||
               message.contains("is a unspecified ip") ||
               message.contains("is a loopback ip") ||
               message.contains("is a private ip")) {
        reply.type = ServiceReplyType.ERROR;
        return;
        
      } else if (message.contains("exceeded the maximum daily limit")) {
        reply.type = ServiceReplyType.LIMIT_REACHED;
        return;
      }
    }
    
    String ip = soup.getString("ip");
    AddressInfos infos = new AddressInfos(ip);
    AddressValidity valid = new AddressValidity(ip, infos);
    AddressType type = new AddressType();
    
    JsonValue security = soup.get("security");
    JsonValue location = soup.get("location");
    JsonValue network = soup.get("network");
  
    // Fill informations about address
    infos.network = network.getString("network");
    String city = location.getString("city");
    infos.location = location.getString("country") + (city.isBlank() ? "" : ", " + city);
    infos.ISP = network.getString("autonomous_system_organization");
    infos.ASN = network.getString("autonomous_system_number");
    infos.locale = location.getString("country_code").toLowerCase();
    infos.longitude = Strings.parseFloat(location.getString("longitude"));
    infos.latitude = Strings.parseFloat(location.getString("latitude"));
    
    // Fill vpn infos
    type.vpn = security.getBoolean("vpn");
    type.proxy = security.getBoolean("proxy");
    type.tor= security.getBoolean("tor");
    type.relay = security.getBoolean("relay");
    type.other = false;
    valid.type = type;
    
    // And return the all
    reply.result = valid;
  }
}
