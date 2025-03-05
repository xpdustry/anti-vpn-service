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
 * You have 1k queries/day with a free account and up to 200k queries/day with the highest, €249/month, plan.
 * 
 * @apiNote This provider is not very accurate and doesn't provide much information.
 */
public class IPHub extends com.xpdustry.avs.service.providers.type.OnlineServiceProvider {
  public IPHub() {
    super("iphub", "IPHub");
    needTokens = true;
    url = "http://v2.api.iphub.info/ip/{0}";
    tokenHeaderName = "X-Key";
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
      String message = result.reply.message.toLowerCase();
      
      result.reply.status = message.contains("empty api key") ? AdvancedHttp.Status.INVALID_TOKEN :
                            message.contains("invalid api key") ? AdvancedHttp.Status.INVALID_TOKEN :
                            message.contains("exceeded your rate limit") ? AdvancedHttp.Status.QUOTA_LIMIT : 
                            AdvancedHttp.Status.ERROR;
      return;
    }
    
    // Some errors are in the ISP key
    String isp = soup.getString("isp");

    if (isp.equalsIgnoreCase("private or bogon ip address")) {
      result.reply.setMessage(isp);
      result.reply.status = AdvancedHttp.Status.ERROR;
      return;
    }
    
    result.result.infos.location = soup.getString("countryName");
    result.result.infos.ASN = soup.getString("asn"); 
    result.result.infos.ISP = isp;
    result.result.infos.locale = com.xpdustry.avs.util.Strings.string2Locale(soup.getString("countryCode"));
    result.result.type.vpn = soup.getInt("block") == 1; //block=2 can be a false positive
  }

  @Override
  public void handleError(AdvancedHttp.Reply reply) {
    if (//reply.httpStatus == 403/*FORBIDDEN*/ ||
        reply.httpStatus == 429/*TO_MANY_REQUESTS*/)
      reply.status = AdvancedHttp.Status.QUOTA_LIMIT;
  }
}
