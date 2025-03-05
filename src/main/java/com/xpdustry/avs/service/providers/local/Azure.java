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

package com.xpdustry.avs.service.providers.local;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.xpdustry.avs.service.providers.type.ProviderType;
import com.xpdustry.avs.util.network.AdvancedHttp;
import com.xpdustry.avs.util.network.Subnet;

import arc.struct.Seq;
import arc.util.serialization.JsonValue;


/** Microsoft Azure's public data-centers and proxies lists. */
public class Azure extends com.xpdustry.avs.service.providers.type.CloudDownloadedProvider {
  public static final Seq<String> urls = Seq.with(
    "https://www.microsoft.com/en-us/download/details.aspx?id=56519",
    "https://www.microsoft.com/en-us/download/details.aspx?id=57062",
    "https://www.microsoft.com/en-us/download/details.aspx?id=57063",
    "https://www.microsoft.com/en-us/download/details.aspx?id=57064"
  );

  /**
   * I know, this is a bad way to scrap an html tag, 
   * but I don't want to increase a lot the size of the plugin with a library, just for that. <br>
   * So I made a strong regex and, in the majority of cases, it's works perfectly.
   */
  private Pattern pattern = Pattern.compile("<a.*href=\"(http[s]?:\\/\\/download\\.microsoft\\.com\\/download\\/.*\\/ServiceTags_.*\\.json?)\".*>"); 
  
  public Azure() {
    super("azure", "Azure Cloud");
    providerType = ProviderType.proxy;
    url = urls.toString("\n");
  }

  @Override
  protected JsonValue downloadList() {
    JsonValue output = new JsonValue(JsonValue.ValueType.array);
    
    urls.each(url -> {
      AdvancedHttp.Reply reply = AdvancedHttp.get(url);
      
      if (reply.isError()) {
        logger.err("avs.provider.cloud.azure.find-link-failed", url);
        if (reply.error != null) logger.err("avs.general-error", reply.error.toString());
        else logger.err("avs.http-status", reply.httpStatus, reply.message);
        return;
      }
      
      Matcher matcher = pattern.matcher(reply.content);
      if (matcher.find() && matcher.groupCount() > 0) {
        String foundUrl = matcher.group(1);
        
        // Fetch the content from the found url
        if (foundUrl != null && !foundUrl.isEmpty()) {
          logger.debug("avs.provider.cloud.azure.downloading", foundUrl);
          output.addChild(downloadFromURL(foundUrl));  
          return;
        } 
      }
      
      logger.err("avs.provider.cloud.azure.find-link-failed", url);
    });
    
    return output;
  }
  
  @Override
  protected Seq<Subnet> extractAddressRanges(JsonValue downloaded) {
    Seq<String> list = new Seq<>();
    
    for (JsonValue values=downloaded.child; values!=null; values=values.next) {
      for (JsonValue entry=values.get("values").child; entry!=null; entry=entry.next) {
        list.addAll(entry.get("properties").get("addressPrefixes").asStringArray());
      }
    }
    
    return list.map(a -> Subnet.createInstance(a));
  }
}
