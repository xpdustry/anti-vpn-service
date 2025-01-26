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

import arc.struct.Seq;
import arc.util.Strings;
import arc.util.serialization.JsonValue;


/** 
 * You have 45 queries/minutes (~64k queries/day) without an account 
 * and unlimited queries with the, €13.3/month, pro plan.
 * 
 * @apiNote Tokens are not supported yet.
 */
public class IPApi extends com.xpdustry.avs.service.providers.type.OnlineServiceProvider {
  public IPApi() {
    super("ip-api", "IP-API");
    canUseTokens = false; // i need to pay to see the documentation for token use
    url = "http://ip-api.com/json/{0}?fields=16968411";
    reavailabilityCheck = CronExpression.createWithoutSeconds("* * * * *");
    reuseCheck = CronExpression.create("1 * * * * *");
  }

  @Override
  public void handleReply(ServiceResult result) {
    JsonValue soup = new arc.util.serialization.JsonReader().parse(result.reply.content);

    if (soup.child == null) {
      result.reply.status = AdvancedHttp.Status.EMPTY_CONTENT;
      return;
    } 
    
    if (soup.getString("status").equals("failed")) {
      result.reply.setMessage(soup.getString("message"));
      String message = result.reply.message.toLowerCase();
      
      result.reply.status = message.contains("invalid/expired key") ? AdvancedHttp.Status.INVALID_TOKEN :
                            AdvancedHttp.Status.ERROR;
      return;
    }
    
    //Check the remaining requests in the header. to avoid an ip ban.
    Seq<String> remains = result.reply.headers.get("X-Rl");
    if (remains != null && !remains.isEmpty() && Strings.parseInt(remains.get(0)) == 0) {
      result.reply.status = AdvancedHttp.Status.QUOTA_LIMIT;
      return;
    }
    
    result.result.infos.location = soup.getString("country") +  ", " 
                                 + soup.getString("regionName") + ", " 
                                 + soup.getString("city");
    String asn = soup.getString("as");
    int i = asn.indexOf(' ');
    if (i != -1) asn = asn.substring(0, i);
    result.result.infos.ASN = asn;
    result.result.infos.ISP = soup.getString("isp");
    result.result.infos.locale = com.xpdustry.avs.util.Strings.string2Locale(soup.getString("countryCode"));
    result.result.infos.longitude = soup.getFloat("lon");
    result.result.infos.latitude = soup.getFloat("lat");
    result.result.type.vpn = soup.getBoolean("hosting");
    result.result.type.proxy = soup.getBoolean("proxy");
  }

  
  @Override
  public void handleError(AdvancedHttp.Reply reply) {
    if (reply.httpStatus == 429/*TO_MANY_REQUESTS*/)
      reply.status = AdvancedHttp.Status.QUOTA_LIMIT;
  }
}
