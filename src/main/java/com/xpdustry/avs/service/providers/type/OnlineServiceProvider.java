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

import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.misc.AVSEvents;
import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.util.network.AdvancedHttp;

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;


public abstract class OnlineServiceProvider extends AddressProvider 
                                            implements ProviderCategories.Onlinable {
  /** Online providers also use a cache, but it is common to all of them. */
  public static final EditableAddressProvider cacheProvider = 
      new com.xpdustry.avs.service.providers.custom.RecentRequestedCache();

  /** The headers to use to make the request. */
  protected final StringMap headers = new StringMap();
  
  /**
   * URL format to use. <br>
   * <code>{0}</code> will be replaced by the address
   */
  protected String url;
  /**
   * URL format to use with a token. <br>
   * <code>{0}</code> will be replaced by the address, <code>{1}</code> replaced by the token.
   */
  protected String urlWithToken;
  /** 
   * Can be defined to use a header for the token instead of {@link #urlWithToken}. <br>
   * Can be used with {@link #urlWithToken}.
   */
  protected String tokenHeaderName;
 
  protected final ObjectMap<String, Integer> tokens = new ObjectMap<>();
  protected boolean canUseTokens = false;
  protected boolean needTokens = false;
  /** 
   * Define whether the service is trusted when reporting that the IP valid, 
   * so the IP will be not checked by other services. 
   */
  protected boolean isTrusted = false; 
  protected int unavailableCooldown = 0;
  
  public OnlineServiceProvider(String name) { super(name); }
  public OnlineServiceProvider(String name, String displayName) { super(name, displayName); }
  
  @SuppressWarnings("unchecked")
  @Override
  public boolean loadMiscSettings() {
    // No tokens needed. skip tokens loading
    if (!canUseTokens()) {
      logger.info("avs.provider.online.loaded");
      return true;
    }

    try {
      Seq<String> tokens = getSettings().getJson("tokens", Seq.class, String.class, Seq::new);
  
      if (tokens.isEmpty() && tokensNeeded()) logger.warn("avs.provider.online.tokens.empty-but-needed");
      else if (tokens.isEmpty()) logger.warn("avs.provider.online.tokens.empty");
      else logger.info("avs.provider.online.tokens.loaded" + (tokens.size > 1 ? "-several" : ""), tokens.size);
      
      this.tokens.clear();
      tokens.each(t -> !t.isBlank(), t -> this.tokens.put(t, 0));
      return true;
      
    } catch(RuntimeException e) {
      logger.err("avs.provider.online.tokens.load-failed");
      logger.err("avs.general-error", e.toString());
      return false;
    } 
  }
  
  @Override
  public boolean reloadMiscSettings() {
    logger.info("avs.provider.online.reload" + (canUseTokens() ? "-with-tokens" : ""));
    return loadMiscSettings();
  }
  
  @Override
  public boolean saveMiscSettings() {
    if (!canUseTokens()) return true;
    
    try {
      getSettings().putJson("tokens", String.class, tokens.keys().toSeq());
      getSettings().save();
      logger.debug("avs.provider.online.tokens.saved");
      return true;
      
    } catch (RuntimeException e) {
      logger.err("avs.provider.online.tokens.write-failed");
      logger.err("avs.general-error", e.toString());
      return false;
    }
  }
  
  @Override
  public int loadedTokens() {
    return tokens.size;
  }

  @Override
  public Seq<String> getTokens() {
    return tokens.keys().toSeq();
  }

  @Override
  public ObjectMap<String, Integer> waitingTokens() {
    ObjectMap<String, Integer> tmp = new ObjectMap<>();
    tokens.each((k, v) -> {if (v > 0) tmp.put(k, v);});
    return tmp;
  }
  
  @Override
  public boolean isProviderAvailable() {
    if(!super.isProviderAvailable()) 
      return false;
    
    else if (unavailableCooldown() > 0) {
      if (--unavailableCooldown <= 0) 
        Events.fire(new AVSEvents.OnlineProviderServiceNowAvailable(this));
      else {
        logger.debug("avs.provider.online.unavailable", unavailableCooldown());
        return false;
      }
      
    } else if (tokensNeeded() && tokens.isEmpty()) {
      logger.debug("avs.provider.online.tokens.empty-but-needed");
      return false;
   
    } else if (tokensNeeded() && !willUseTokens()) {
      logger.debug("avs.provider.online.tokens.no-availables-but-needed");
      refreshTokens();
      return false;
    }

    refreshTokens();
    return true;
  }

  @Override
  public boolean isTrusted() {
    return isTrusted;
  }
  
  @Override
  public String getURL() {
    return willUseTokens() ? urlWithToken : url;
  }

  @Override
  public boolean tokensNeeded() {
    // little fix
    if (needTokens && !canUseTokens) canUseTokens = true;
    return needTokens;
  }

  @Override
  public boolean canUseTokens() {
    // little fix
    if (needTokens && !canUseTokens) canUseTokens = true;
    return canUseTokens;
  }

  @Override
  public boolean willUseTokens() {
    if (canUseTokens() && !tokens.isEmpty()) {
      for (ObjectMap.Entry<String, Integer> token : tokens.entries()) {
        if (token.value <= 0) return true;
      }
    }
    return false;
  }
  
  protected void refreshTokens() {
    tokens.each((k, v) -> {
      if (v > 0) {
        tokens.put(k, v-1);
        if (v-1 <= 0) Events.fire(new AVSEvents.OnlineProviderTokenNowAvailable(this, k));
      }
    });
  }

  @Override
  public int unavailableCooldown() {
    return unavailableCooldown;
  }

  @Override
  public void makeAvailable() {
    if (unavailableCooldown > 0) {
      unavailableCooldown = 0;
      Events.fire(new AVSEvents.OnlineProviderServiceNowAvailable(this));     
    }
  }
  
  @Override
  public boolean addToken(String token) {
    token = token.strip();
    if (!canUseTokens() || tokens.containsKey(token)) return false;
    tokens.put(token, 0);
    Events.fire(new AVSEvents.OnlineProviderAddedTokenEvent(this, token));
    save();
    return true;
  }
  
  @Override
  public boolean removeToken(String token) {
    token = token.strip();
    if (!canUseTokens() || !tokens.containsKey(token)) return false;
    tokens.remove(token);
    Events.fire(new AVSEvents.OnlineProviderRemovedTokenEvent(this, token));
    save();
    return true;
  }
  
  @Override
  public boolean makeTokenAvailable(String token) {
    token = token.strip();
    if (!canUseTokens() || !tokens.containsKey(token)) return false;
    Integer last = tokens.put(token, 0);
    if (last == null || last > 0)
      Events.fire(new AVSEvents.OnlineProviderTokenNowAvailable(this, token));
    return true;
  }
  
  @Override
  public void checkAddressImpl(AddressProviderReply reply) {
    // First, search in the cache
    AddressValidity cached = cacheProvider.get(reply.address);
    if (cached != null) {
      reply.validity = cached;
      reply.type = AddressProviderReply.ReplyType.FOUND;
      return;
    }
    
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
      if (!tokensNeeded()) logger.warn("avs.provider.online.tokens.without");
    }
    
    if (!tokensNeeded()) {
      ServiceResult result = request(reply.address, null);
      
      if (result == null || result.isError()) 
        reply.type = AddressProviderReply.ReplyType.ERROR;
      else {
        cacheProvider.add(result.result);
        reply.setResult(result.result);
      }      
    }
  }
  
  /** @return whether to continue the iteration or not */
  protected boolean checkAddressWithToken(AddressProviderReply reply, String token, int tokenTimeout) {
    if (tokenTimeout > 0) {
      logger.debug("avs.provider.online.token.unavailable", tokenTimeout);
      return true;
    }
    
    ServiceResult result = request(reply.address, token);
    
    if (result == null || result.isError()) {
      reply.type = AddressProviderReply.ReplyType.ERROR;
      return (result != null &&
              result.reply.status == AdvancedHttp.Status.INVALID_TOKEN &&
              result.reply.status == AdvancedHttp.Status.QUOTA_LIMIT);
    }
    
    cacheProvider.add(result.result);
    reply.setResult(result.result);
    return false;
  }

  protected ServiceResult request(String ip, @arc.util.Nullable String token) {
    if (token != null) {
      if (tokenHeaderName == null && urlWithToken == null) {
        logger.err("avs.provider.online.missing-token.msg1");
        logger.err("avs.provider.online.missing-token.msg2");
        return null;
      }      
    } else if (url == null) {
      logger.err("avs.provider.online.missing.msg1");
      logger.err("avs.provider.online.missing.msg2");
      return null;
    }
    
    if (tokenHeaderName != null) {
      if (token == null) headers.remove(tokenHeaderName);
      else headers.put(tokenHeaderName, token);   
    }
    
    String formattedUrl = token != null && urlWithToken != null ? 
        MessageFormat.format(urlWithToken, ip, token) :
        MessageFormat.format(url, ip);
 
    AdvancedHttp.Reply reply = AdvancedHttp.get(formattedUrl, headers);
    Events.fire(new AVSEvents.OnlineProviderServiceRequest(this, reply));
    ServiceResult result = new ServiceResult(reply, ip);
    
    if (!reply.isError()) {
      try { handleReply(result); }
      catch (Exception e) {
        logger.err("avs.provider.online.error", ip, e.toString());
        result.setError(); 
        int v = AVSConfig.serviceCheckCooldown.getInt();
        if (v > 0) {
          unavailableCooldown = v;
          logger.warn("avs.provider.online.in-waiting-list", v);
          Events.fire(new AVSEvents.OnlineProviderServiceNowUnavailable(this));        
        }
        return result;
      }
    } else handleError(reply);
    
    // Token specific
    if ((reply.status == AdvancedHttp.Status.INVALID_TOKEN ||
        reply.status == AdvancedHttp.Status.QUOTA_LIMIT) && token != null) {
      logger.err("avs.provider.online.token." + 
                (reply.status == AdvancedHttp.Status.QUOTA_LIMIT ? "use-limit" : "invalid"), ip, token);
      logger.err("avs.http-status", reply.httpStatus.code, reply.message);
      result.setError();
      int v = AVSConfig.tokenCheckCooldown.getInt();
      if (v > 0) {
        tokens.put(token, v);
        logger.warn("avs.provider.online.token.in-waiting-list", v);
        Events.fire(new AVSEvents.OnlineProviderTokenNowUnavailable(this, token));        
      }
      
    // General case
    } else if (reply.error != null || reply.status == AdvancedHttp.Status.ERROR) {
      logger.err("avs.provider.online.error", ip, reply.message);
      if (token != null) logger.warn("avs.provider.online.token.skipped");
      result.setError();
    
    // Service specific
    } else if (reply.status.isFatalError()) {
      logger.err("avs.provider.online.service-error", ip);
      logger.err("avs.http-status", reply.httpStatus.code, reply.message);
      result.setError();
      int v = AVSConfig.serviceCheckCooldown.getInt();
      if (v > 0) {
        unavailableCooldown = v;
        logger.warn("avs.provider.online.in-waiting-list", v);
        Events.fire(new AVSEvents.OnlineProviderServiceNowUnavailable(this));        
      }
    }
    
    return result;
  }

  public abstract void handleReply(ServiceResult result);
  
  /** Can be overrides to handle errors, like to change the status on a specific http code */
  public void handleError(AdvancedHttp.Reply reply) {
  }
  
  public static class ServiceResult {
    public final AddressValidity result;
    public final AdvancedHttp.Reply reply;
    public final String ip;
    private boolean error;

    public ServiceResult(AdvancedHttp.Reply reply, String ip) {
      this.reply = reply;
      this.ip = ip;
      this.result = new AddressValidity(ip, new com.xpdustry.avs.misc.address.AddressInfos(ip));
    }
    
    public boolean isError() {
      return error;
    }

    public void setError() {
      error = true;
    }    
  }
}
