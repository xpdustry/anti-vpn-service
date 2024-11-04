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

package com.xpdustry.avs.service.providers.online;

import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.network.AdvancedHttp;

import arc.util.serialization.JsonValue;

// Others service idea: https://github.com/xpdustry/nucleus/issues/10
public class VpnApiService extends com.xpdustry.avs.service.providers.type.OnlineServiceProvider {
  public VpnApiService() {
    super("vpnapi", "VpnAPI.io");
    canUseTokens = true;
    url = "https://vpnapi.io/api/{0}";
    urlWithToken = "https://vpnapi.io/api/{0}?key={1}";
    isTrusted = true;
  }

  @Override
  public void handleReply(ServiceResult result) {
    JsonValue soup = new arc.util.serialization.JsonReader().parse(result.reply.result);
    
    if (soup.child == null) {
      result.reply.status = AdvancedHttp.Status.EMPTY_CONTENT;
      return;
      
    } else if (soup.has("message")) {
      result.reply.setMessage(soup.getString("message"));
      String message = result.reply.message.toLowerCase();
      
      if (message.contains("invalid api key")) {
        result.reply.status = AdvancedHttp.Status.INVALID_TOKEN;
        return;
        
      } else if (message.contains("is not a valid ip") ||
                 message.contains("is a unspecified ip") ||
                 message.contains("is a loopback ip") ||
                 message.contains("is a private ip")) {
        result.reply.status = AdvancedHttp.Status.ERROR;
        return;
        
      } else if (message.contains("exceeded the maximum daily limit")) {
        result.reply.status = AdvancedHttp.Status.QUOTA_LIMIT;
        return;
      }
    }

    JsonValue security = soup.get("security");
    JsonValue location = soup.get("location");
    JsonValue network = soup.get("network");
  
    // Fill informations about address
    result.result.infos.network = network.getString("network");
    JsonValue c = location.get("city");
    String city = c == null ? "" : c.asString();
    result.result.infos.location = location.getString("country") + (city.isBlank() ? "" : ", " + city);
    result.result.infos.ISP = network.getString("autonomous_system_organization");
    result.result.infos.ASN = network.getString("autonomous_system_number");
    result.result.infos.locale = location.getString("country_code").toLowerCase();
    result.result.infos.longitude = Strings.parseFloat(location.getString("longitude"));
    result.result.infos.latitude = Strings.parseFloat(location.getString("latitude"));
    
    // Fill vpn infos
    result.result.type.vpn = security.getBoolean("vpn");
    result.result.type.proxy = security.getBoolean("proxy");
    result.result.type.tor= security.getBoolean("tor");
    result.result.type.relay = security.getBoolean("relay");
  }
  
  @Override
  public void handleError(AdvancedHttp.Reply reply) {
    if (reply.httpStatus.code == 403) 
      reply.status = AdvancedHttp.Status.QUOTA_LIMIT;
  }
}
