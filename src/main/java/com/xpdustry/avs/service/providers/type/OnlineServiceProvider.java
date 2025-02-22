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

package com.xpdustry.avs.service.providers.type;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.misc.AVSEvents;
import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.util.CronExpression;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.network.AdvancedHttp;

import arc.Events;
import arc.struct.OrderedMap;
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
  /** 
   * Define whether the service is trusted when reporting that the IP valid, 
   * so the IP will be not checked by other services. 
   */
  protected boolean isTrusted = false; 
  /**
   * Define if the service can use tokens.
   */
  protected boolean canUseTokens = false;
  /**
   * Define if the service need tokens to work.
   */
  protected boolean needTokens = false; 
  /**
   * Programmed period between before considering a service as re-available, 
   * when not, if {@link AdvancedHttp.Status#isFatalError()} is {@code true}.
   * @apiNote the time check is done when {@link #isAvailable()} is called, 
   *          so before every ip address checks when {@link #checkAddress(String)} is called.
   * @apiNote {@link AdvancedHttp.Status#ERROR} is ignored.
   */
  protected CronExpression reavailabilityCheck;
  /**
   * Programmed period between before reusing a service or a token, when ran out of queries,
   * after received a {@link AdvancedHttp.Status#QUOTA_LIMIT} or {@link AdvancedHttp.Status#INVALID_TOKEN}.
   * @apiNote {@link AdvancedHttp.Status#ERROR} is ignored.
   */
  protected CronExpression reuseCheck;

  /**
   * Time before service's re-availability. {@code null} if currently available.
   */
  private ZonedDateTime unavailability;
  private final OrderedMap<String, ZonedDateTime> tokens = new OrderedMap<>();

  public OnlineServiceProvider(String name) { super(name); }
  public OnlineServiceProvider(String name, String displayName) { super(name, displayName); }
  
  @SuppressWarnings("unchecked")
  @Override
  public boolean loadMiscSettings() {
    // check properties
    if (reavailabilityCheck == null || reuseCheck == null) {
      logger.err("avs.provider.online.missing-cooldown.msg1");
      logger.err("avs.provider.online.missing-cooldown.msg2");
      return false;
    } else if (canUseTokens() && tokenHeaderName == null && urlWithToken == null) {
      logger.err("avs.provider.online.missing-token.msg1");
      logger.err("avs.provider.online.missing-token.msg2");
      return false;
    } else if (!tokensNeeded() && url == null) {
      logger.err("avs.provider.online.missing-url.msg1");
      logger.err("avs.provider.online.missing-url.msg2");
      return false;
    }
    
    // No tokens needed. skip tokens loading
    if (!canUseTokens()) {
      logger.info("avs.provider.online.loaded");
      return true;
    }

    try {
      Seq<String> tokens = getSettings().get("tokens", Seq.class, String.class, new Seq<>());
  
      if (tokens.isEmpty() && tokensNeeded()) logger.warn("avs.provider.online.tokens.empty-but-needed");
      else if (tokens.isEmpty()) logger.warn("avs.provider.online.tokens.empty");
      else logger.info("avs.provider.online.tokens.loaded" + (tokens.size > 1 ? "-several" : ""), tokens.size);
      
      this.tokens.clear();
      tokens.each(t -> !t.isBlank(), t -> this.tokens.put(t, null));
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
      getSettings().put("tokens", String.class, tokens.keys().toSeq());
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
  public Seq<String> waitingTokens() {
    Seq<String> tmp = new Seq<>();
    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    tokens.each((k, v) -> {if (now.isAfter(v)) tmp.add(k);});
    return tmp;
  }
  
  @Override
  public boolean isAvailable() {
    if(!super.isAvailable()) 
      return false;
    
    else if (unavailability() != null) {
      ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
      if (now.isAfter(unavailability())) makeAvailable();
      else {
        long duration = Duration.between(now, unavailability()).toMillis();
        logger.debug("avs.provider.online.unavailable", 
                     Strings.duration2str(logger, duration, false, 2));
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
  
  protected void refreshTokens() {
    if (!canUseTokens()) return;
    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    tokens.each((k, v) -> {
      if (v != null && now.isAfter(v)) makeTokenAvailable(k);
    });
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
      for (OrderedMap.Entry<String, ZonedDateTime> token : tokens.entries()) {
        if (token.value == null) return true;
      }
    }
    return false;
  }

  @Override
  public ZonedDateTime unavailability() {
    return unavailability;
  } 

  @Override
  public void makeAvailable() {
    if (unavailability() != null) {
      unavailability = null;
      logger.info("avs.provider.online.available");
      Events.fire(new AVSEvents.OnlineProviderServiceNowAvailable(this));     
    }
  }
  
  @Override
  public void makeUnavailable() {
    makeUnavailable(reuseCheck); 
  }
  
  protected void makeUnavailable(CronExpression cooldown) {
    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    unavailability = cooldown.nextTimeAfter(now);
    Duration duration = Duration.between(now, unavailability);
    
    logger.warn("avs.provider.online.in-waiting-list", 
                Strings.duration2str(logger, duration.toMillis(), false, 2));
    Events.fire(new AVSEvents.OnlineProviderServiceNowUnavailable(this, duration));      
  }
  
  @Override
  public boolean addToken(String token) {
    if (!canUseTokens() || tokens.containsKey(token)) return false;
    tokens.put(token, null);
    Events.fire(new AVSEvents.OnlineProviderAddedTokenEvent(this, token));
    save();
    return true;
  }
  
  @Override
  public boolean removeToken(String token) {
    if (!canUseTokens() || !tokens.containsKey(token)) return false;
    tokens.remove(token);
    Events.fire(new AVSEvents.OnlineProviderRemovedTokenEvent(this, token));
    save();
    return true;
  }
  
  @Override
  public boolean makeTokenAvailable(String token) {
    if (!canUseTokens() || !tokens.containsKey(token)) return false;
    if (tokens.put(token, null) != null) {
      logger.info("avs.provider.online.token.available", token);
      Events.fire(new AVSEvents.OnlineProviderTokenNowAvailable(this, token));
    }  
    return true;
  }
  
  @Override
  public boolean makeTokenUnavailable(String token) {
    if (!canUseTokens() || !tokens.containsKey(token)) return false;

    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC),
                  next = reuseCheck.nextTimeAfter(now);
    Duration duration = Duration.between(now, next);

    tokens.put(token, next);
    logger.warn("avs.provider.online.token.in-waiting-list", 
                Strings.duration2str(logger, duration.toMillis(), false, 2));
    Events.fire(new AVSEvents.OnlineProviderTokenNowUnavailable(this, token, duration));
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
      ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
      
      if (AVSConfig.randomTokens.getBool()) { 
        if (tokens.orderedKeys().copy().shuffle().contains(t -> !checkAddressWithToken(reply, t, tokens.get(t), now)))
          return;
      } else {
        for (OrderedMap.Entry<String, ZonedDateTime> token : tokens.entries()) {
          if (!checkAddressWithToken(reply, token.key, token.value, now)) return;
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
  protected boolean checkAddressWithToken(AddressProviderReply reply, String token, 
                                          ZonedDateTime unavailability, ZonedDateTime now) {
    if (unavailability != null) {
      long duration = Duration.between(now, unavailability).toMillis();
      logger.debug("avs.provider.online.token.unavailable", 
                   Strings.duration2str(logger, duration, false, 2));
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
        makeUnavailable();
        return result;
      }
    } else handleError(reply);
    
    
    if ((reply.status == AdvancedHttp.Status.INVALID_TOKEN ||
        reply.status == AdvancedHttp.Status.QUOTA_LIMIT) && token != null) {
      logger.err("avs.provider.online.token." + 
                (reply.status == AdvancedHttp.Status.QUOTA_LIMIT ? "use-limit" : "invalid"), ip, token);
      logger.err("avs.http-status", reply.httpStatus, reply.message);
      result.setError();
      makeTokenUnavailable(token);

    } else if (reply.status == AdvancedHttp.Status.QUOTA_LIMIT) {
      logger.err("avs.provider.online.service-limit", ip);
      logger.err("avs.http-status", reply.httpStatus, reply.message);
      result.setError();
      makeUnavailable();  
      
    } else if (reply.error != null || reply.status == AdvancedHttp.Status.ERROR) {
      logger.err("avs.provider.online.error", ip, reply.message);
      if (token != null) logger.warn("avs.provider.online.token.skipped");
      result.setError();
    
    } else if (reply.status.isFatalError()) {
      logger.err("avs.provider.online.service-error", ip);
      logger.err("avs.http-status", reply.httpStatus, reply.message);
      result.setError();
      makeUnavailable(reavailabilityCheck);     
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
