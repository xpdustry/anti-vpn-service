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

package com.xpdustry.avs.service.providers.type;

import java.text.MessageFormat;

import com.xpdustry.avs.misc.AVSConfig;
import com.xpdustry.avs.misc.address.AddressInfos;
import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.util.network.AdvancedHttp;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;


public abstract class OnlineServiceProvider extends AddressProvider {
  /** Online providers also use a cache, but it is common to all of them. */
  public static final EditableAddressProvider cacheProvider = 
      new com.xpdustry.avs.service.providers.custom.RecentRequestedCache();

  /**
   * The URL of service to use. 
   * Don't forgot to add '{0}' = the ip, '{1}' = the token (optional), in the URL.
   */
  public final String url;
  /** The url of service, to use when 'needTokens' is false and no tokens in list. */
  public String urlWithoutToken = "";
  /** The headers to use to make the request. */
  protected arc.struct.StringMap headers;
 
  protected ObjectMap<String, Integer> tokens = new ObjectMap<>();
  protected boolean canUseTokens = false;
  protected boolean needTokens = false;
  /** 
   * Define whether the service is trusted when reporting that the IP valid, 
   * so the IP will be not checked by other services. 
   */
  protected boolean isTrusted = false; 
  protected int unavailableTimeout = 0;
  
  public OnlineServiceProvider(String displayName, String url) { 
    super(displayName);
    if (url == null || url.isBlank()) throw new NullPointerException("url is empty");
    this.url = url.strip();
    folder = AVSConfig.tokensDirectory.get();
    fileExt = "txt";
  }
  
  public OnlineServiceProvider(String displayName, String name, String url) { 
    super(displayName, name); 
    if (url == null || url.isBlank()) throw new NullPointerException("url is empty");
    this.url = url.strip();
    folder = AVSConfig.tokensDirectory.get();
    fileExt = "txt";
  }
  
  @Override
  public boolean load() {
    loaded = false;
    // No tokens needed. skip tokens loading
    if (!canUseTokens()) {
      if (urlWithoutToken.isBlank()) urlWithoutToken = url;
      logger.info("avs.provider.online.loaded");
      loaded = true;
      return loaded;
    
    } else if (!tokensNeeded() && urlWithoutToken.isBlank()) urlWithoutToken = url;
    
    loaded = loadTokens();
    return loaded;
  }
  
  @Override
  public boolean reload() {
    loaded = false;
    logger.info("avs.provider.online.reload" + (canUseTokens() ? "-with-tokens" : ""));
    tokens.clear();
    return load();
  }
  
  @Override
  public boolean save() {
    return saveTokens();
  }
  
  protected boolean loadTokens() {
    Fi tokensFile = getFile();
    Seq<String> tokens_ = new Seq<>();
    
    if (tokensFile.exists() && !tokensFile.isDirectory()) {
      try { tokens_ = Seq.with(tokensFile.readString().split("\n")).map(line -> line.strip()); }
      catch (Exception e) { 
        logger.err("avs.provider.online.tokens.load-failed", tokensFile.path());
        logger.err("avs.general-error", e.toString());     
        return false;
      }
    } else saveTokens();
    
    tokens_.removeAll(v -> v.isEmpty() || v.startsWith("#"));
    tokens_.each(t -> tokens.put(t, 0));
    
    if (tokens.isEmpty() && tokensNeeded()) logger.warn("avs.provider.online.tokens.empty-but-needed");
    else if (tokens.isEmpty()) logger.warn("avs.provider.online.tokens.empty");
    else logger.info("avs.provider.online.tokens.loaded" + (tokens.size > 1 ? "-several" : ""), tokens.size);
    
    return true;
  }
  
  protected boolean saveTokens() {
    // No tokens needed. skip this
    if (!canUseTokens()) return true;
    
    Fi tokensFile = getFile();
    
    try {
      tokensFile.writeString("# " + logger.getKey("avs.provider.online.tokens.file-comment") + "\n");
      tokens.each((t, v) -> tokensFile.writeString(t + "\n", true));
      
    } catch (Exception e) {
      logger.err("avs.provider.online.tokens.write-failed", tokensFile.path());
      logger.err("avs.general-error", e.toString());
      return false;
    }
    
    return true;
  }
  
  public int loadedTokens() {
    return tokens.size;
  }
  
  @Override
  public boolean isProviderAvailable() {
    if(!super.isProviderAvailable()) 
      return false;
    
    else if (--unavailableTimeout > 0) {
      logger.debug("avs.provider.online.unavailable", unavailableTimeout());
      return false;
      
    } else if (tokensNeeded() && tokens.isEmpty()) {
      logger.debug("avs.provider.online.tokens.empty-but-needed");
      return false;
   
    } else if (tokensNeeded() && !willUseTokens()) {
      logger.debug("avs.provider.online.tokens.no-availables-but-needed");
      // and remove one to all tokens
      tokens.each((k, v) -> tokens.put(k, v-1));
      return false;
    }

    return true;
  }
  
  public boolean isTrusted() {
    return isTrusted;
  }
  
  public boolean tokensNeeded() {
    return needTokens;
  }
  
  public boolean canUseTokens() {
    return canUseTokens;
  }
  
  public boolean willUseTokens() {
    if (canUseTokens() && !tokens.isEmpty()) {
      for (ObjectMap.Entry<String, Integer> token : tokens.entries()) {
        if (token.value <= 0) return true;
      }
    }
    return false;
  }
  
  public int unavailableTimeout() {
    return unavailableTimeout;
  }
  
  public void makeAvailable() {
    unavailableTimeout = 0;
  }
  
  /** @return whether the token has been added */
  public boolean addToken(String token) {
    token = token.strip();
    if (!canUseTokens() || tokens.containsKey(token)) return false;
    tokens.put(token, 0);
    save();
    return true;
  }
  
  /** @return whether the token has been removed */
  public boolean removeToken(String token) {
    token = token.strip();
    if (!canUseTokens() || !tokens.containsKey(token)) return false;
    tokens.remove(token);
    save();
    return true;
  }
  
  /** @return whether the token unavailability has been reset */
  public boolean makeTokenAvailable(String token) {
    token = token.strip();
    if (!canUseTokens() || !tokens.containsKey(token)) return false;
    tokens.put(token, 0);
    save();
    return true;
  }
  
  @Override
  public void checkAddressImpl(AddressProviderReply reply) {
    if (willUseTokens()) {
      if (AVSConfig.randomTokens.getBool() && tokens.size > 1) { 
        if (tokens.keys().toSeq().shuffle().contains(t -> !checkAddressWithToken(reply, t, tokens.get(t))))
          return;
      } else {
        for (ObjectMap.Entry<String, Integer> token : tokens.entries()) {
          if (!checkAddressWithToken(reply, token.key, token.value)) return;
        }  
      }

      // No valid token
      logger.warn("avs.provider.online.tokens.all-skipped");
    }
    
    ServiceResult result = request(reply.address, null);
    
    if (result.isError()) reply.type = AddressProviderReply.ReplyType.ERROR;
    else {
      cacheProvider.add(result.result);
      reply.setResult(result.result);
    }
  }
  
  /** @return whether to continue the iteration or not */
  protected boolean checkAddressWithToken(AddressProviderReply reply, String token, int tokenTimeout) {
    if (tokenTimeout > 0) {
      tokens.put(token, tokenTimeout-1);
      logger.debug("avs.provider.online.token.unavailable", tokenTimeout-1);
      
    } else {
      ServiceResult result = request(reply.address, token);
      
      if (result.isError()) {
        reply.type = AddressProviderReply.ReplyType.ERROR;
        if (result.reply.status != AdvancedHttp.Status.INVALID_TOKEN &&
            result.reply.status != AdvancedHttp.Status.QUOTA_LIMIT) 
          return false;
        
      } else {
        cacheProvider.add(result.result);
        reply.setResult(result.result);
        return false;
      }
    }
    
    return true;
  }

  protected ServiceResult request(String ip, @arc.util.Nullable String token) {
    String formattedUrl = token == null ? MessageFormat.format(urlWithoutToken, ip) : 
                                          MessageFormat.format(url, ip, token);
    AdvancedHttp.Reply reply = AdvancedHttp.get(formattedUrl);
    ServiceResult result = new ServiceResult(reply, ip);
    
    if (!reply.isError()) {
      try { handleReply(result); }
      catch (Exception e) {
        logger.err("avs.provider.online.error", ip, e.toString());
        unavailableTimeout = AVSConfig.serviceCheckTimeout.getInt();
        logger.warn("avs.provider.online.in-waiting-list", unavailableTimeout);
        result.setError();
        return result;
      }
    }
    
    // Token specific
    if ((reply.status == AdvancedHttp.Status.INVALID_TOKEN ||
        reply.status == AdvancedHttp.Status.QUOTA_LIMIT) && token != null) {
      logger.err("avs.provider.online.token." + 
                (reply.status == AdvancedHttp.Status.QUOTA_LIMIT ? "use-limit" : "invalid"), ip, token);
      logger.err("avs.http-status", reply.httpStatus.code, reply.message);
      tokens.put(token, AVSConfig.tokenCheckTimeout.get());
      logger.warn("avs.provider.online.token.in-waiting-list", AVSConfig.tokenCheckTimeout.get());
      result.setError();
     
    // General case
    } else if (reply.error != null || reply.status == AdvancedHttp.Status.ERROR) {
      logger.err("avs.provider.online.error", ip, reply.message);
      if (token != null) logger.warn("avs.provider.online.token.skipped");
      result.setError();
    
    // Server specific
    } else if (reply.status.isFatalError()) {
      logger.err("avs.provider.online.service-error", ip);
      logger.err("avs.http-status", reply.httpStatus.code, reply.message);
      unavailableTimeout = AVSConfig.serviceCheckTimeout.get();
      logger.warn("avs.provider.online.in-waiting-list", unavailableTimeout);
      result.setError();
    }
    
    return result;
  }

  public abstract void handleReply(ServiceResult result);
  
  
  public class ServiceResult {
    public final AddressValidity result;
    public final AdvancedHttp.Reply reply;
    public final String ip;
    private boolean error;

    public ServiceResult(AdvancedHttp.Reply reply, String ip) {
      this.reply = reply;
      this.ip = ip;
      this.result = new AddressValidity(ip, new AddressInfos(ip));
    }
    
    public boolean isError() {
      return error;
    }

    public void setError() {
      error = true;
    }    
  }
}
