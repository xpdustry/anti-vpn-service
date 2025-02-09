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

package com.xpdustry.avs.service.providers;

import com.xpdustry.avs.service.providers.type.AddressProvider;
import com.xpdustry.avs.service.providers.type.ProviderCategories;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.logging.Logger;

import arc.func.Cons2;
import arc.func.Cons3;
import arc.struct.Seq;


public enum ProviderAction {
  enable(Category.common, ActionCallbacks::enable),
  disable(Category.common, ActionCallbacks::disable),
  reload(Category.common, (a, l) -> a.reload()),
  
  list(Category.cached, ActionCallbacks::list),
  search(Category.cached, ActionCallbacks::search),
  info(Category.cached, ActionCallbacks::info),
  
  refresh(Category.cloud, ActionCallbacks::refresh),
  
  add(Category.editable, ActionCallbacks::add),
  remove(Category.editable, ActionCallbacks::remove),
  clear(Category.editable, (a, l) -> ((ProviderCategories.Editable) a).clear()),

  //check(Category.online, CallbackKeper::onlineCheckAction),
  addToken(Category.online, ActionCallbacks::addToken),
  delToken(Category.online, ActionCallbacks::delToken),
  listTokens(Category.online, ActionCallbacks::listTokens),
  ;

  public static final Seq<ProviderAction> all = Seq.with(values());
  public static final String actionDescKeyFormat = "avs.provider.action.@.@",
                             actionArgsKeyFormat = actionDescKeyFormat + ".args";
  
  public final String name;
  public final Category category;
  private final Cons2<ProviderCategories.Basic, Logger> run;
  private final Cons3<ProviderCategories.Basic, String, Logger> run2;
  
  ProviderAction(Category category, Cons2<ProviderCategories.Basic, Logger> run) {
    this.name = Strings.camelToKebab(name());
    this.category = category;
    this.run = run;
    this.run2 = null;
  }
  
  ProviderAction(String name, Category category, Cons2<ProviderCategories.Basic, Logger> run) {
    this.name = name;
    this.category = category;
    this.run = run;
    this.run2 = null;
  }
  
  ProviderAction(Category category, Cons3<ProviderCategories.Basic, String, Logger> run) {
    this.name = Strings.camelToKebab(name());
    this.category = category;
    this.run = null;
    this.run2 = run;
  }
  
  ProviderAction(String name, Category category, Cons3<ProviderCategories.Basic, String, Logger> run) {
    this.name = name;
    this.category = category;
    this.run = null;
    this.run2 = run;
  }

  /*private void add() {
    if (all.contains(a -> a.name.equals(name)))
      throw new IllegalStateException("duplicate '" + name +"' action");
    all.add(this);
  }*/
  
  public boolean argRequired() {
    return run2 != null && run == null;
  }
  
  private void check(AddressProvider provider, boolean withArgs) {
    if (withArgs && !argRequired())
      throw new IllegalArgumentException("no arguments are expected. "
                                       + "Use the other .run() method instead");
    else if (!withArgs && argRequired())
      throw new IllegalArgumentException("an argument is expected. "
                                       + "Use the other .run() method instead");
    else if (!category.clazz.isInstance(provider))
      throw new IllegalArgumentException("incompatible provider type " + provider.getClass().getName()
                                       + ". Must implement " + category.clazz.getName());
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
    return logger.getKey(Strings.format(actionDescKeyFormat, category.name, name));
  }
  
  public String getArgs(Logger logger) {
    if (!argRequired()) return "";
    return logger.getKey(Strings.format(actionArgsKeyFormat, category.name, name));
  }
  
  @Override
  public String toString() {
    return name;
  }

  public static ProviderAction get(String name) {
    return all.find(a -> a.name.equals(name));
  }
  
  public static Seq<ProviderAction> getAll(Category category) {
    return all.select(a -> a.category.equals(category));
  }
  
  public static Seq<ProviderAction> getAll(AddressProvider provider) {
    Seq<ProviderAction> result = new Seq<>();
    Category.getAll(provider).each(c -> result.addAll(getAll(c)));
    return result;
  }

  
  public static enum Category {
    common(ProviderCategories.Basic.class),
    cached(ProviderCategories.Cacheable.class),
    cloud(ProviderCategories.Cloudable.class),
    editable(ProviderCategories.Editable.class),
    online(ProviderCategories.Onlinable.class);
    
    public static final Seq<Category> all = Seq.with(values()); 
    public static final String categoryKeyFormat = "avs.provider.category.@";
    
    public final String name;
    public final Class<? extends ProviderCategories.Basic> clazz;
    
    Category(Class<? extends ProviderCategories.Basic> clazz) { 
      this.name = Strings.camelToKebab(name());
      if (clazz != ProviderCategories.Basic.class && (!clazz.isInterface() || 
          !Seq.with(clazz.getInterfaces()).contains(ProviderCategories.Basic.class))) 
        throw new IllegalArgumentException(name + ": the category class must be an interface "
                                         + "inherited from ProviderCategories.Basic");
      this.clazz = clazz;
    }
    
    Category(String name, Class<? extends ProviderCategories.Basic> clazz) {
      this.name = name;
      if (!clazz.isInterface() || 
          !Seq.with(clazz.getInterfaces()).contains(ProviderCategories.Basic.class)) 
        throw new IllegalArgumentException(name + ": the category class must be an interface "
                                         + "inherited from ProviderCategories.Basic");
      this.clazz = clazz;
    }
    
    /*private void add() {
      if (all.contains(a -> a.name.equals(name)))
        throw new IllegalStateException("duplicate '" + name +"' action category");
      all.add(this);
    }*/
    
    public String getDesc(Logger logger) {
      return logger.getKey(Strings.format(categoryKeyFormat, name));
    }
    
    
    public static Category get(String name) {
      return all.find(c -> c.name.equals(name));
    }
    
    public static Seq<Category> getAll(AddressProvider provider) {
      Seq<Category> result = new Seq<>();
      Seq<Class<?>> interfaces = new Seq<>();
      Class<?> look = provider.getClass();

      while (look != null && look != Object.class) {
        for (Class<?> c : look.getInterfaces()) {
          if (ProviderCategories.Basic.class.isAssignableFrom(c) && !interfaces.contains(c))
            interfaces.add(c);
        }
        look = look.getSuperclass();
      }

      int i, c;
      for (i=interfaces.size-1; i>=0; i--) {
        for (c=all.size-1; c>=0; c--) {
          if (interfaces.get(i) == all.get(c).clazz) {
            result.add(all.get(c));
            break;
          }
        }  
      }

      return result;
    }
  }
}
