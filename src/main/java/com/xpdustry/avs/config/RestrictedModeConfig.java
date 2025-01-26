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

package com.xpdustry.avs.config;

import com.xpdustry.avs.command.AVSCommandManager;
import com.xpdustry.avs.command.Command;
import com.xpdustry.avs.config.abstracts.ChangeValider;
import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.providers.ProviderAction;
import com.xpdustry.avs.service.providers.type.AddressProvider;

import arc.files.Fi;
import arc.func.Func;
import arc.struct.ObjectMap;
import arc.struct.Seq;


public class RestrictedModeConfig extends com.xpdustry.avs.config.abstracts.AbstractConfig {
  private static final RestrictedModeConfig INSTANCE = new RestrictedModeConfig();
  
  public static final Field<Boolean> enabled = new Field<>("enabled", true);
  public static final FieldList<AddressProvider> providers =  new FieldList<>("providers", 
      Default.providers,  n -> AntiVpnService.allProviders.find(p -> p.name.equals(n)), f -> f.name);
  public static final FieldList<AVSConfig.Field> settings = new FieldList<>("settings", 
      Default.settings, n -> (AVSConfig.Field) AVSConfig.instance().all.find(f -> f.name.equals(n)), s -> s.name);
  public static final FieldList<Command> commands = new FieldList<>("commands", 
      Default.commands, n -> AVSCommandManager.subCommands.find(c -> c.name.equals(n)), c -> c.name);
  
  private RestrictedModeConfig() { super("restrict", true); }
  public static RestrictedModeConfig instance() { return INSTANCE; }

  @Override
  protected String msgBundleKey(String key) {
    return "avs." + name + "." + key;
  }
  
  @Override
  protected Fi getFile() {
    return AVSConfig.subDir(AVSConfig.settingsDirectory.getString()).child(name + ".json");
  }
  
  @Override
  protected void loadMisc() {
    all.each(s -> s.get());   

    // Set default provider actions
    for (ObjectMap.Entry<AddressProvider, Seq<ProviderAction>> e : Default.actions.entries()) {
      if (e.key.actions.isEmpty()) {
        e.key.actions = e.value.copy();
        if (!e.key.save()) {
          config = null;
          return;
        }
      }
    }
  }
  
  public boolean save() {
    try { 
      all.each(s -> s instanceof FieldList, s -> ((FieldList<?>) s).save());
      logger.info(msgBundleKey("saved"));
      return true;
    
    } catch (Exception e) {
      logger.info(msgBundleKey("save-failed"), config.getFile().path());
      logger.err("avs.general-error", e.toString());
      return false;
    }
  }
  
  
  public static class Default {
    public static final Seq<AddressProvider> providers = AntiVpnService.allProviders.select(a ->
        a instanceof com.xpdustry.avs.service.providers.custom.Whitelist ||
        a instanceof com.xpdustry.avs.service.providers.custom.Blacklist
    );
    
    public static final Seq<AVSConfig.Field> settings = Seq.with(
        AVSConfig.connectLimit,
        AVSConfig.kickMessage,
        AVSConfig.serverBusyMessage,
        AVSConfig.errorMessage
    );
    
    public static final Seq<Command> commands = AVSCommandManager.subCommands.select(c -> 
        c instanceof com.xpdustry.avs.command.list.ConfigCommand ||
        c instanceof com.xpdustry.avs.command.list.ProviderCommand ||
        c instanceof com.xpdustry.avs.command.list.InfoCommand ||
        c instanceof com.xpdustry.avs.command.list.HelpCommand
    );
    
    public static final ObjectMap<AddressProvider, Seq<ProviderAction>> actions = 
        providers.asMap(p -> p, p -> ProviderAction.getAll(ProviderAction.Category.cached)
                                                   .addAll(ProviderAction.add, ProviderAction.remove));  
  }

  
  public static class Field<T> extends com.xpdustry.avs.config.abstracts.AbstractField<T> {
    public static final String bundleDescFormat = "avs." + INSTANCE.name + ".field.@";
    
    public Field(String name, T defaultValue) { super(INSTANCE, name, defaultValue); }
    public Field(String name, T defaultValue, ChangeValider<T> validate) {
      super(INSTANCE, name, defaultValue, validate);
    }

    @Override
    protected String descKeyFormat() { return bundleDescFormat; }
  }
  
  public static class FieldList<T> extends com.xpdustry.avs.config.abstracts.AbstractFieldListSerialized<T> {
    public FieldList(String name, Seq<T> defaultValue, Func<String, T> loader, Func<T, String> saver) {
      super(INSTANCE, name, defaultValue, loader, saver);
    }
    public FieldList(String name, Seq<T> defaultValue, ChangeValider<Seq<T>> validate, 
                     Func<String, T> loader, Func<T, String> saver) {
      super(INSTANCE, name, defaultValue, validate, loader, saver);
    }

    @Override
    protected String descKeyFormat() { return Field.bundleDescFormat; }
    @Override
    public void load() {
      super.load();
      
      if (values.contains((T) null)) {
        values.clear();
        throw new NullPointerException(name + ": found invalid item(s) in values");    
      }
    }
    @Override
    public void save() {
      if (values.contains((T) null)) 
        throw new NullPointerException(name + ": found invalid item(s) in values");
      super.save();
    }
    @Override
    public String toString() { return "[\n" + values.toString(",\n", saver) + "\n]"; }
  }
}
