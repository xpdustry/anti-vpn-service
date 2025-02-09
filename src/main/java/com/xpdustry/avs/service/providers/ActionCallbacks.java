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

package com.xpdustry.avs.service.providers;

import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.service.providers.type.ProviderCategories;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.logging.Logger;

import arc.struct.Seq;

import static com.xpdustry.avs.service.providers.ProviderAction.*;


/** Class to store {@link ProviderAction} callbacks. */
class ActionCallbacks {
  private static String key(ProviderAction action, String key) {
    return Strings.format(actionDescKeyFormat + ".@", action.category.name, action.name, key);
  }
  
  
  static void enable(ProviderCategories.Basic provider, Logger logger) {
    if (provider.isEnabled()) logger.err(key(enable, "already"));
    else provider.enable();
  }
  
  static void disable(ProviderCategories.Basic provider, Logger logger) {
    if (!provider.isEnabled()) logger.err(key(disable, "already"));
    else provider.disable();
  }
  
  static void list(ProviderCategories.Basic provider, Logger logger) {
    Seq<String> result = ((ProviderCategories.Cacheable) provider).list().map(s -> s.toString());
    
    if (result.isEmpty()) {
      logger.warn(key(list, "empty"));
      return;
    }
    
    StringBuilder builder = new StringBuilder(logger.getKey(key(list, "head"))).append('\n');

    if (result.size > 10) {
      boolean toolong = false;
      int rest = 0, max = 100;
      
      if (result.size > max) {
        rest = result.size - max;
        result.setSize(max);
        toolong = true;
      }
      
      Seq<String> table = Strings.tableify(result, 70);
      if (toolong) table.add(logger.formatKey(key(list, "and-more"), rest));
      
      table.each(l -> builder.append("&lk|&fr ").append(l.strip()).append('\n'));
              
    } else result.each(s -> builder.append("&lk|&fr ").append(s).append('\n'));

    logger.infoNormal(builder.toString());
  }
  
  static void search(ProviderCategories.Basic provider, String arg, Logger logger) {
    try { AddressValidity.checkAddress(arg); }
    catch (IllegalArgumentException e) {
      logger.err(key(search, "invalid"), arg);
      return;
    }
    
    Seq<AddressValidity> result = ((ProviderCategories.Cacheable) provider).matches(arg);
    
    if (result.isEmpty()) {
      logger.err(key(search, "not-found"), arg);
      return;
    }
    
    StringBuilder builder = new StringBuilder();
    
    builder.append(logger.formatKey(key(search, "matches"), result.size, arg)).append('\n');
    if (result.size > 10) {
      boolean toolong = false;
      int rest = 0, max = 100;
      
      if (result.size > max) {
        rest = result.size - max;
        result.setSize(max);
        toolong = true;
      }
      
      Seq<String> table = Strings.tableify(result.map(v -> v.subnet.toString()), 70);
      if (toolong) table.add(logger.formatKey(key(list, "and-more"), rest));
      
      table.each(l -> builder.append("&lk|&fr ").append(l.strip()).append('\n'));
    
    } else result.each(v -> builder.append("&lk|&fr ").append(v.subnet.toString()).append('\n'));
    
    logger.infoNormal(builder.toString());
  }
  
  static void info(ProviderCategories.Basic provider, String arg, Logger logger) {
    try { AddressValidity.checkSubnet(arg); }
    catch (IllegalArgumentException e) {
      logger.err(key(info, "invalid"), arg);
      return;
    }
    
    AddressValidity result = ((ProviderCategories.Cacheable) provider).get(arg);
    
    if (result == null) {
      logger.err(key(info, "not-found"), arg);
      return;
    }
    
    StringBuilder builder = new StringBuilder();
    
    builder.append(logger.formatKey(key(info, "match"), arg)).append('\n');
    result.toFormattedString(builder, logger, false);
    logger.infoNormal(builder.toString());
  }
  
  static void refresh(ProviderCategories.Basic provider, Logger logger) {
    logger.info(key(refresh, "wait"));
    ((ProviderCategories.Cloudable) provider).refresh();
  }
  
  static void add(ProviderCategories.Basic provider, String arg, Logger logger) {
    String[] args = arg.split(" ");
    
    try { AddressValidity.checkSubnet(args[0]); }
    catch (IllegalArgumentException e) {
      logger.err(key(info, "invalid"), arg);
      return;
    }
    
    AddressValidity address = new AddressValidity(args[0]);
    if (args.length > 1) {
      for (int i=1; i<args.length; i++) {
        switch (args[i].toLowerCase()) {
          case "other": address.type.other = true; break;
          case "vpn": address.type.vpn = true; break;
          case "proxy": address.type.proxy = true; break;
          case "tor": address.type.tor = true; break;
          case "relay": address.type.relay = true; break;
          case "datacenter": address.type.dataCenter = true; break;
          default: logger.err(key(add, "invalid-type")); return;
        }
      }
    }
    
    if (((ProviderCategories.Editable) provider).add(address))
         logger.info(key(add, "added"));
    else logger.err(key(add, "present"));
  }
  
  static void remove(ProviderCategories.Basic provider, String arg, Logger logger) {
    try { AddressValidity.checkSubnet(arg); }
    catch (IllegalArgumentException e) {
      logger.err(key(info, "invalid"), arg);
      return;
    }
    
    if (((ProviderCategories.Editable) provider).remove(new AddressValidity(arg)))
      logger.info(key(remove, "removed"));
    else logger.err(key(remove, "not-in"));
  }
  
/*
  static void onlineCheckAction(ProviderCategories.Basic provider, String arg, Logger logger) {
    
  }
*/
  
  static void addToken(ProviderCategories.Basic provider, String arg, Logger logger) {
    if (!((ProviderCategories.Onlinable) provider).canUseTokens()) {
      logger.err(key(listTokens, "no-use"));
      return;
    }
    
    if (((ProviderCategories.Onlinable) provider).addToken(arg))
      logger.info(key(addToken, "added"));
    else logger.err(key(addToken, "present"));
  }
  
  static void delToken(ProviderCategories.Basic provider, String arg, Logger logger) {
    if (!((ProviderCategories.Onlinable) provider).canUseTokens()) {
      logger.err(key(listTokens, "no-use"));
      return;
    }
    
    if (((ProviderCategories.Onlinable) provider).removeToken(arg))
      logger.info(key(delToken, "added"));
    else logger.err(key(delToken, "not-in"));
  }
  
  static void listTokens(ProviderCategories.Basic provider, Logger logger) {
    if (!((ProviderCategories.Onlinable) provider).canUseTokens()) {
      logger.warn(key(listTokens, "no-use"));
      return;
    }
    
    Seq<String> result = ((ProviderCategories.Onlinable) provider).getTokens();
    
    if (result.isEmpty()) {
      logger.warn(key(list, "empty"));
      return;
    }
    
    StringBuilder builder = new StringBuilder(logger.getKey(key(listTokens, "head"))).append('\n');
    result.each(t -> builder.append("&lk|&fr ").append(t).append('\n'));
    
    logger.infoNormal(builder.toString());
  }
}
