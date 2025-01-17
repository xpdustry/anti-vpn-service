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
 * You have 100 queries/day without an account, 1k queries/day with a free account
 * and up to 10M queries/day for the highest, $200/month, plan.
 */
public class ProxyCheck extends com.xpdustry.avs.service.providers.type.OnlineServiceProvider {
  public ProxyCheck() {
    super("proxycheck", "ProxyCheck.io");
    canUseTokens = true;
    url = "https://proxycheck.io/v2/{0}?vpn=3&asn=1&cur=0&short=1&p=0&days=365";
    urlWithToken = "https://proxycheck.io/v2/{0}?key={1}&vpn=3&asn=1&cur=0&short=1&p=0&days=365";
    //isTrusted = true; //not sure of that
  }

  @Override
  public void handleReply(ServiceResult result) {
    JsonValue soup = new arc.util.serialization.JsonReader().parse(result.reply.result);

    if (soup.child == null) {
      result.reply.status = AdvancedHttp.Status.EMPTY_CONTENT;
      return;
    } 
    
    result.reply.setMessage(soup.getString("message", null));
    String message = result.reply.message != null ? result.reply.message.toLowerCase() : null;
    switch (soup.getString("status")) {
      default: 
      case "ok":
        break;
        
      case "warning":
        break;
        
      case "denied":
        
        return;
        
      case "error":
        result.reply.status = AdvancedHttp.Status.ERROR;
        return;
    }
    
    
  }
}
