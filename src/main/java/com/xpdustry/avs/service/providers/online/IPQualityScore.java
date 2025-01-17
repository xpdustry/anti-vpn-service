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
 * You have 1k queries/month with a free account and 75k queries/month with the highest, $999/month, plan.
 */
public class IPQualityScore extends com.xpdustry.avs.service.providers.type.OnlineServiceProvider {
  public IPQualityScore() {
    super("ipqs", "IP Quality Score");
    needTokens = true;
    urlWithToken = "https://ipqualityscore.com/api/json/ip/{1}/{0}?strictness=1";
    isTrusted = true;
  }

  @Override
  public void handleReply(ServiceResult result) {
    JsonValue soup = new arc.util.serialization.JsonReader().parse(result.reply.result);

    if (soup.child == null) {
      result.reply.status = AdvancedHttp.Status.EMPTY_CONTENT;
      return;

    } else if (!soup.getBoolean("success", false)) {
      String message = soup.getString("message", "");
      if (!message.isEmpty()) result.reply.setMessage(message);
      message = message.toLowerCase();
      
      result.reply.status = message.contains("unauthorized key") ? AdvancedHttp.Status.INVALID_TOKEN :
                            message.contains("insufficient credits") ? AdvancedHttp.Status.QUOTA_LIMIT : 
                            AdvancedHttp.Status.ERROR;
      return;
      
    }
    
    // Some errors are in the ISP key
    String isp = soup.getString("ISP");

    if (isp.equalsIgnoreCase("private ip address") || 
        isp.equalsIgnoreCase("reserved") || 
        isp.equalsIgnoreCase("unassigned") ||
        isp.equalsIgnoreCase("n/a")) {
      result.reply.setMessage(isp);
      result.reply.status = AdvancedHttp.Status.ERROR;
      return;
    }
    
    // Fill informations about address
    result.result.infos.location = soup.getString("region", "n/a") + ", " + soup.getString("city", "n/a");
    result.result.infos.ASN = soup.getString("ASN"); 
    result.result.infos.ISP = isp;
    result.result.infos.locale = java.util.Locale.forLanguageTag(soup.getString("country_code"));
    result.result.infos.longitude = soup.getFloat("longitude");
    result.result.infos.latitude = soup.getFloat("latitude");
    
    // Fill vpn infos
    result.result.type.vpn = soup.getBoolean("vpn");
    result.result.type.proxy = soup.getBoolean("proxy");
    result.result.type.tor= soup.getBoolean("tor");
    result.result.type.other = soup.getBoolean("is_crawler", false);
  }

}
