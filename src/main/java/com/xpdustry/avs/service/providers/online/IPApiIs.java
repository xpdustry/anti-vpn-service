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

import com.xpdustry.avs.util.CronExpression;
import com.xpdustry.avs.util.network.AdvancedHttp;

import arc.util.serialization.JsonValue;

/**
 * You have 100 queries/day without an account, 1k queries/day with a free account
 * and up to 2M queries/day with the highest, $200/month, plan.
 */
public class IPApiIs extends com.xpdustry.avs.service.providers.type.OnlineServiceProvider {
  public IPApiIs() {
    super("ipapi", "ipapi.is");
    canUseTokens = true;
    url = "https://api.ipapi.is/?q={0}";
    urlWithToken = "https://api.ipapi.is/?q={0}&key={1}";
    reavailabilityCheck = CronExpression.createWithoutSeconds("0 * * * *");
    reuseCheck = CronExpression.createWithoutSeconds("0 1 * * *");
  }

  @Override
  public void handleReply(ServiceResult result) {
    JsonValue soup = new arc.util.serialization.JsonReader().parse(result.reply.content);
    JsonValue tmp;

    if (soup.child == null) {
      result.reply.status = AdvancedHttp.Status.EMPTY_CONTENT;
      return;
      
    } else if ((tmp = soup.get("error")) != null) {
      result.reply.setMessage(tmp.asString());
      if ((tmp = soup.get("message")) != null) 
        result.reply.setMessage(tmp.asString());
      
      String message = result.reply.message.toLowerCase();
      
      result.reply.status = message.contains("too many api requests") ? AdvancedHttp.Status.QUOTA_LIMIT :
                            message.contains("please register at") ? AdvancedHttp.Status.INVALID_TOKEN :
                            AdvancedHttp.Status.ERROR;
      return;
      
    } else if (soup.getBoolean("is_bogon")) {
      result.reply.status = AdvancedHttp.Status.ERROR;
      return;
    }
    

    // Fill informations about address
    tmp = soup.get("location");
    if (tmp != null) {
      result.result.infos.location = tmp.getString("country") + ", "
                                   + tmp.getString("state") + ", " 
                                   + tmp.getString("city");
      result.result.infos.locale = com.xpdustry.avs.util.Strings.string2Locale(tmp.getString("country_code"));
      result.result.infos.longitude = tmp.getFloat("longitude");
      result.result.infos.latitude = tmp.getFloat("latitude"); 
    }
    tmp = soup.get("asn");
    if (tmp != null) {
      result.result.infos.ASN = tmp.getString("asn");
      result.result.infos.ISP = tmp.getString("org");      
    }

    // Fill vpn infos
    result.result.type.vpn = soup.getBoolean("is_vpn");
    result.result.type.proxy = soup.getBoolean("is_proxy");
    result.result.type.tor= soup.getBoolean("is_tor");
    result.result.type.dataCenter = soup.getBoolean("is_datacenter");
    result.result.type.other = //soup.getBoolean("is_abuser") || //can make false positives
                               soup.getBoolean("is_crawler");
  }
}
