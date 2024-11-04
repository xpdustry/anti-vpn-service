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

package com.xpdustry.avs.config;

import com.xpdustry.avs.command.AVSCommandManager;
import com.xpdustry.avs.command.Command;
import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.providers.ProviderAction;
import com.xpdustry.avs.service.providers.type.AddressProvider;
import com.xpdustry.avs.util.DynamicSettings;
import com.xpdustry.avs.util.Logger;

import arc.struct.ObjectMap;
import arc.struct.Seq;


public class RestrictedModeConfig {
  private static Logger logger = new Logger("Restrict");
  private static DynamicSettings config;
  private static boolean loaded = false;
  
  public static boolean enabled = true;
  public static final Seq<AddressProvider> providers = new Seq<>();
  public static final Seq<ConfigField> settings = new Seq<>();
  public static final Seq<Command> commands = new Seq<>();
  
  public static boolean isLoaded() {
    return loaded;
  }

  @SuppressWarnings("unchecked")
  public static boolean load() {
    loaded = false;
    
    arc.files.Fi file = AVSConfig.subDir(AVSConfig.settingsDirectory.getString()).child("restrict.json");
    config = new DynamicSettings(file, true);
    
    try {
      config.load();
      
      enabled = config.getBool("enabled", enabled);
      
      providers.clear();
      config.getJson("providers", Seq.class, String.class, () -> Default.providers.map(p -> p.name))
            .each(n -> providers.add(AntiVpnService.allProviders.find(p -> p.name.equals(n))));
      if (providers.contains((AddressProvider) null)) {
        logger.err("avs.restrict.field.providers.invalid");
        providers.removeAll(p -> p == null);
        return loaded;
      }
      
      settings.clear();
      config.getJson("settings", Seq.class, String.class, () -> Default.settings.map(f -> f.name))
            .each(n -> settings.add(AVSConfig.all.find(f -> f.name.equals(n))));
      if (settings.contains((ConfigField) null)) {
        logger.err("avs.restrict.field.settings.invalid");
        settings.removeAll(p -> p == null);
        return loaded;
      }
      
      commands.clear();
      config.getJson("commands", Seq.class, String.class, () -> Default.commands.map(c -> c.name))
            .each(n -> commands.add(AVSCommandManager.subCommands.find(c -> c.name.equals(n))));
      if (commands.contains((Command) null)) {
        logger.err("avs.restrict.field.commands.invalid");
        commands.removeAll(p -> p == null);
        return loaded;
      }
      
    } catch (RuntimeException e) {
      logger.info("avs.restrict.load-failed", config.getFile().path());
      logger.err("avs.general-error", e);
      return loaded;
    }

    // Set default provider actions
    for (ObjectMap.Entry<AddressProvider, Seq<ProviderAction>> e : Default.actions.entries()) {
      if (e.key.actions.isEmpty()) {
        e.key.actions = e.value.copy();
        if (!e.key.save()) return loaded;
      }
    }
    
    logger.info("avs.restrict.loaded");
    loaded = true;
    return loaded;
  }
  
  public static boolean save() {
    try {
      config.put("enabled", enabled);
      config.putJson("providers", AddressProvider.class, providers);
      config.putJson("settings", ConfigField.class, settings);
      config.putJson("commands", Command.class, commands); 
    
    } catch (RuntimeException e) {
      logger.info("avs.restrict.save-failed", config.getFile().path());
      logger.err("avs.general-error", e.toString());
      return false;
    }

    logger.info("avs.restrict.saved");
    return true;
  }
  
  
  public static class Default {
    public static final Seq<AddressProvider> providers = AntiVpnService.allProviders.select(a ->
        a instanceof com.xpdustry.avs.service.providers.custom.Whitelist ||
        a instanceof com.xpdustry.avs.service.providers.custom.Blacklist
    );
    
    public static final Seq<ConfigField> settings = Seq.with(
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
}
