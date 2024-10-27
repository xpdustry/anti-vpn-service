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

package com.xpdustry.avs.service.providers;

import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.service.providers.type.*;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.Strings;

import arc.func.Cons2;
import arc.func.Cons3;
import arc.struct.Seq;

/** TODO: make action configurable per provider */
public enum ProviderAction {
  reload(Category.common, (a, l) -> a.reload()),
  enable(Category.common, CallbackKeper::commonEnableAction),
  disable(Category.common, CallbackKeper::commonDisableAction),
  
  list(Category.cached, CallbackKeper::cachedListAction),
  search(Category.cached, CallbackKeper::cachedSearchAction),
  info(Category.cached, CallbackKeper::cachedInfoAction),
  
  refresh(Category.cloud, CallbackKeper::cloudRefreshAction),
  
  add(Category.editable, CallbackKeper::editableAddAction),
  remove(Category.editable, CallbackKeper::editableRemoveAction),
  clear(Category.editable, (a, l) -> ((EditableAddressProvider) a).clear()),
  
  //check(Category.online, CallbackKeper::onlineCheckAction),
  addToken(Category.online, CallbackKeper::onlineAddTokenAction),
  delToken(Category.online, CallbackKeper::onlineDelTokenAction),
  listTokens(Category.online, CallbackKeper::onlineListTokensAction),
  ;
  
  public static final Seq<ProviderAction> all = Seq.with(values());
  public static final String actionKeyPrefix = "avs.provider.action.";
  
  public final String name;
  public final Category category;
  private final Cons2<AddressProvider, Logger> run;
  private final Cons3<AddressProvider, String, Logger> run2;
  
  ProviderAction(Category category, Cons2<AddressProvider, Logger> run) {
    this.name = Strings.camelToKebab(name());
    this.category = category;
    this.run = run;
    this.run2 = null;
  }
  
  ProviderAction(Category category, Cons3<AddressProvider, String, Logger> run) {
    this.name = Strings.camelToKebab(name());
    this.category = category;
    this.run = null;
    this.run2 = run;
  }

  public boolean argRequired() {
    return run2 != null && run == null;
  }
  
  private void check(AddressProvider provider, boolean withArgs) {
    if (withArgs && !argRequired())
      throw new IllegalArgumentException("no arguments are expected. "
                                       + "Use the other .exec() method instead");
    else if (!withArgs && argRequired())
      throw new IllegalArgumentException("an argument is expected. "
                                       + "Use the other .exec() method instead");
    else if (!category.clazz.isInstance(provider))
      throw new IllegalArgumentException("incompatible provider type " + provider.getClass().getName()
                                       + ". Must be an instance of " + category.clazz.getName());
  }
  
  public void run(AddressProvider provider, String arg, Logger logger) {
    check(provider, true);
    provider.exec(() -> run2.get(provider, arg, logger), logger);
  }

  public void run(AddressProvider provider, Logger logger) {
    check(provider, false);
    provider.exec(() -> run.get(provider, logger), logger);
  }
  
  public String getDesc(Logger logger) {
    return logger.getKey(actionKeyPrefix + name);
  }
  
  
  public static ProviderAction get(String name) {
    return all.find(a -> a.name.equals(name));
  }
  
  public static Seq<ProviderAction> getAll(Category category) {
    return all.select(a -> a.category.equals(category));
  }

  
  public static enum Category {
    common(AddressProvider.class),
    cached(CachedAddressProvider.class),
    cloud(CloudDownloadedProvider.class),
    editable(EditableAddressProvider.class),
    online(OnlineServiceProvider.class);
    
    public static final Seq<Category> all = Seq.with(values()); 
    public static final String categoryKeyPrefix = "avs.provider.category.";
    
    public final String name;
    public final Class<? extends AddressProvider> clazz;
    
    Category(Class<? extends AddressProvider> clazz) { 
      this.name = name();
      this.clazz = clazz;
    }
    
    Category(String name, Class<? extends AddressProvider> clazz) {
      this.name = name;
      this.clazz = clazz;
    }
    
    public String getDesc(Logger logger) {
      return logger.getKey(categoryKeyPrefix + name);
    }
    
    
    public static Category get(String name) {
      return all.find(c -> c.name.equals(name));
    }
    
    public static Category get(AddressProvider provider) {
      for (int i=all.size-1; i>=0; i--) {
        if (all.get(i).clazz.isInstance(provider))
          return all.get(i);
      }
      return null;
    }
    
    public static Seq<Category> getAll(AddressProvider provider) {
      Seq<Category> result = new Seq<>();
      Class<?> look = provider.getClass();
      
      while (look != null && look != Object.class) {
        for (int i=all.size-1; i>=0; i--) {
          if (look == all.get(i).clazz) {
            result.add(all.get(i));
            look = look.getSuperclass();
          }
        }
        if (look != null) look = look.getSuperclass();
      }

      return result;
    }
  }

  
  /** Private class to store multi-lines callbacks, for a better visual in action definition */
  private static class CallbackKeper {
    private static String key(ProviderAction action, String key) {
      return actionKeyPrefix + action.name + "." + key;
    }
    
    
    private static void commonEnableAction(AddressProvider provider, Logger logger) {
      if (provider.isEnabled()) logger.err(key(enable, "already"));
      else provider.enable();
    }
    
    private static void commonDisableAction(AddressProvider provider, Logger logger) {
      if (!provider.isEnabled()) logger.err(key(disable, "already"));
      else provider.disable();
    }
    
    private static void cachedListAction(AddressProvider provider, Logger logger) {
      Seq<String> result = ((CachedAddressProvider) provider).list().map(s -> s.toString());
      
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
        
        Seq<String> table = Strings.tableify(result, 3, false);
        if (toolong) table.add(logger.formatKey(key(list, "and-more"), rest));
        
        table.each(l -> builder.append("&lk|&fr ").append(l.strip()).append('\n'));
                
      } else result.each(s -> builder.append("&lk|&fr ").append(s).append('\n'));

      logger.infoNormal(builder.toString());
    }
    
    private static void cachedSearchAction(AddressProvider provider, String arg, Logger logger) {
      Seq<AddressValidity> result = ((CachedAddressProvider) provider).matches(arg);
      
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
        
        Seq<String> table = Strings.tableify(result.map(v -> v.subnet.toString()), 3, false);
        if (toolong) table.add(logger.formatKey(key(list, "and-more"), rest));
        
        table.each(l -> builder.append("&lk|&fr ").append(l.strip()).append('\n'));
      
      } else result.each(v -> builder.append("&lk|&fr ").append(v.subnet.toString()).append('\n'));
      
      logger.infoNormal(builder.toString());
    }
    
    private static void cachedInfoAction(AddressProvider provider, String arg, Logger logger) {
      AddressValidity result = ((CachedAddressProvider) provider).get(arg);
      
      if (result == null) {
        logger.err(key(info, "not-found"), arg);
        return;
      }
      
      StringBuilder builder = new StringBuilder();
      
      builder.append(logger.formatKey(key(info, "match"), arg)).append('\n');
      result.toFormattedString(builder, logger, false);
      logger.infoNormal(builder.toString());
    }
    
    private static void cloudRefreshAction(AddressProvider provider, Logger logger) {
      logger.info(key(refresh, "wait"));
      ((CloudDownloadedProvider) provider).refresh();
    }
    
    private static void editableAddAction(AddressProvider provider, String arg, Logger logger) {
      if (((EditableAddressProvider) provider).add(new AddressValidity(arg)))
           logger.info(key(add, "added"));
      else logger.err(key(add, "present"));
    }
    
    private static void editableRemoveAction(AddressProvider provider, String arg, Logger logger) {
      if (((EditableAddressProvider) provider).remove(new AddressValidity(arg)))
        logger.info(key(remove, "removed"));
      else logger.err(key(remove, "not-in"));
    }
    
/*
    private static void onlineCheckAction(AddressProvider provider, String arg, Logger logger) {
      
    }
*/
    
    private static void onlineAddTokenAction(AddressProvider provider, String arg, Logger logger) {
      if (!((OnlineServiceProvider) provider).canUseTokens()) {
        logger.err(key(listTokens, "no-use"));
        return;
      }
      
      if (((OnlineServiceProvider) provider).addToken(arg))
        logger.info(key(addToken, "added"));
      else logger.err(key(addToken, "present"));
    }
    
    private static void onlineDelTokenAction(AddressProvider provider, String arg, Logger logger) {
      if (!((OnlineServiceProvider) provider).canUseTokens()) {
        logger.err(key(listTokens, "no-use"));
        return;
      }
      
      if (((OnlineServiceProvider) provider).removeToken(arg))
        logger.info(key(delToken, "added"));
      else logger.err(key(delToken, "not-in"));
    }
    
    private static void onlineListTokensAction(AddressProvider provider, Logger logger) {
      if (!((OnlineServiceProvider) provider).canUseTokens()) {
        logger.warn(key(listTokens, "no-use"));
        return;
      }
      
      Seq<String> result = ((OnlineServiceProvider) provider).getTokens();
      
      if (result.isEmpty()) {
        logger.warn(key(list, "empty"));
        return;
      }
      
      StringBuilder builder = new StringBuilder(logger.getKey(key(listTokens, "head"))).append('\n');
      result.each(t -> builder.append("&lk|&fr ").append(t).append('\n'));
      
      logger.infoNormal(builder.toString());
    }
  }
}
