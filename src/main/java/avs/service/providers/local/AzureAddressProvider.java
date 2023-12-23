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

package avs.service.providers.local;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


import arc.struct.Seq;
import arc.struct.OrderedSet;
import arc.util.serialization.JsonValue;
import avs.util.network.AwaitHttp;
import avs.util.network.Subnet;


public class AzureAddressProvider extends avs.service.providers.types.CloudDownloadedAddressProvider {
  public static final Seq<String> urls = Seq.with(
    "https://www.microsoft.com/en-us/download/details.aspx?id=56519",
    "https://www.microsoft.com/en-us/download/details.aspx?id=57062",
    "https://www.microsoft.com/en-us/download/details.aspx?id=57063",
    "https://www.microsoft.com/en-us/download/details.aspx?id=57064"
  );
  
  private String foundUrl = "", error = "unknown error";
  // I know, this is a bad way to scrap an html tag, but I don't want to increase a lot the size of the plugin with a library, just for that.
  // I made a strong regex, so in the majority of cases, it's works perfectly.
  private Pattern pattern = Pattern.compile("<a.*href=\"(http[s]?:\\/\\/download\\.microsoft\\.com\\/download\\/.*\\/ServiceTags_.*\\.json?)\".*>"); 
  
  public AzureAddressProvider() {
    super("Azure Cloud", "azure", urls.toString("\n"));
    this.providerType = CloudAddressProviderType.proxy;
  }

  @Override
  public JsonValue downloadList() {
    JsonValue output = new JsonValue(JsonValue.ValueType.array);
    
    urls.each(url -> {
      AwaitHttp.get(url, success -> {
        if (success.getStatus() == AwaitHttp.HttpStatus.OK) {
          Matcher matcher = pattern.matcher(success.getResultAsString());
          if (matcher.find() && matcher.groupCount() > 0) foundUrl = matcher.group(1);
          
        } else error = success.getStatus().code + " " + success.getStatus();
      }, failure -> error = failure.toString());

      if (foundUrl == null || foundUrl.isBlank()) {
        logger.warn("Unable to find @ download link at url '@'. Skipping...", displayName, url);
        logger.warn("Error: " + error);
        return;
      } else logger.debug("Downloading list at url '@'...", foundUrl);
      
      AwaitHttp.get(foundUrl, success -> {
        try {
          if (success.getStatus() == AwaitHttp.HttpStatus.OK) output.addChild(new arc.util.serialization.JsonReader().parse(success.getResultAsString().strip()));
          else {
            logger.err("Failed to fetch list from url '@'.", foundUrl);
            logger.err("Status: @ '@'", success.getStatus().code, success.getStatus().toString().replace('_', ' '));
          }
        } catch (Exception e) {
          logger.err("Failed to decode fecthed content from url '@'.", foundUrl);
          logger.err("Status: @ '@'", success.getStatus().code, e.toString());
        }
      }, failure -> {
        logger.err("Failed to fetch list from url '@'.", foundUrl);
        if (failure instanceof AwaitHttp.HttpStatusException status) {
          String message = status.response.getResultAsString();
          if (message.isBlank()) message = status.getLocalizedMessage();
          else message = message.strip();
          logger.err("Status: @ '@'", status.status.code, message);
        } else 
          logger.err("Status: @ '@'", AwaitHttp.HttpStatus.UNKNOWN_STATUS.code, failure.toString());
      });
    });
    
    return output;
  }
  
  @Override
  public Seq<Subnet> extractAddressRanges(JsonValue downloaded) {
    OrderedSet<String> list = new OrderedSet<>();
    
    for (JsonValue values=downloaded.child; values!=null; values=values.next) {
      for (JsonValue entry=values.get("values").child; entry!=null; entry=entry.next) {
        list.addAll(entry.get("properties").get("addressPrefixes").asStringArray());
      }
    }
    
    return list.toSeq().map(a -> Subnet.createInstance(a)).removeAll(a -> a == null);
  }
}
