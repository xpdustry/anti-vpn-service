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

package com.xpdustry.avs.service.providers.online;

import com.xpdustry.avs.util.network.AdvancedHttp;

import arc.util.serialization.JsonValue;

/**
 * You have 500 queries/month without an account, 30k queries/month with a free account
 * and up to 600k queries/month with the highest, $499/month, plan.
 * 
 * @apiNote The service doesn't give accurate information until you have the highest plan.
 */
public class IP2Location extends com.xpdustry.avs.service.providers.type.OnlineServiceProvider {
  public IP2Location() {
    super("ip2location", "IP2Location.io");
    canUseTokens = true;
    url = "https://api.ip2location.io/?ip={0}&format=json";
    urlWithToken = "https://api.ip2location.io/?key={1}&ip={0}&format=json";
  }

  @Override
  public void handleReply(ServiceResult result) {
    JsonValue soup = new arc.util.serialization.JsonReader().parse(result.reply.result);
    JsonValue tmp;

    if (soup.child == null) {
      result.reply.status = AdvancedHttp.Status.EMPTY_CONTENT;
      return;
      
    } else if ((tmp = soup.get("error")) != null) {
      result.reply.setMessage(tmp.asString());
      String message = result.reply.message.toLowerCase();
      
      result.reply.status = message.contains("empty api key") ? AdvancedHttp.Status.INVALID_TOKEN :
                            message.contains("invalid api key") ? AdvancedHttp.Status.INVALID_TOKEN :
                            message.contains("insufficient query") ? AdvancedHttp.Status.QUOTA_LIMIT : 
                            message.contains("internal server error") ? AdvancedHttp.Status.SERVER_ERROR :
                            AdvancedHttp.Status.ERROR;
      return;
    }
    
    // There is no message to know if the ip address is valid, all returned fields are null or "-".
    // I will use the 'asn' key to know if the address is valid.
    // Edit: there are informations but only with a paid plan.
    
    String asn = soup.getString("asn");
    
    if (asn == null || asn.equals("-")) {
      result.reply.status = AdvancedHttp.Status.ERROR;
      return;
    }
    
    result.result.infos.location = soup.getString("country_name") + ", " 
                                 + soup.getString("region_name")+ ", " 
                                 + soup.getString("city_name");
    result.result.infos.ASN = soup.getString("asn");
    result.result.infos.ISP = soup.getString("isp", soup.getString("as", result.result.infos.ISP));
    result.result.infos.locale = java.util.Locale.forLanguageTag(soup.getString("country_code"));
    result.result.infos.longitude = soup.getFloat("longitude");
    result.result.infos.latitude = soup.getFloat("latitude");
    
    // Without the highest plan, we doesn't have the 'proxy' key, that store the type of address
    tmp = soup.get("proxy");
    
    if (tmp == null) result.result.type.proxy = soup.getBoolean("is_proxy", false);
    else {
      result.result.type.vpn = tmp.getBoolean("is_vpn", false);
      result.result.type.proxy = tmp.getBoolean("is_public_proxy", false) || 
                                 tmp.getBoolean("is_web_proxy", false) ||
                                 tmp.getBoolean("is_residential_proxy", false);
      result.result.type.tor= tmp.getBoolean("is_tor", false);
      result.result.type.relay = tmp.getBoolean("is_consumer_privacy_network", false) ||
                                 tmp.getBoolean("is_enterprise_private_network", false);  
      result.result.type.dataCenter = tmp.getBoolean("is_data_center", false);
      result.result.type.other = tmp.getBoolean("is_web_crawler", false) || 
                                 tmp.getBoolean("is_spammer", false) ||
                                 tmp.getBoolean("is_scanner", false) ||
                                 tmp.getBoolean("is_botnet", false);
    }
  }

}
