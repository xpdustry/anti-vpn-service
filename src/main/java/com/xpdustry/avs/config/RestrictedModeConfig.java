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
import com.xpdustry.avs.command.list.*;
import com.xpdustry.avs.config.base.*;
import com.xpdustry.avs.misc.ProviderActionSeq;
import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.providers.ProviderAction;
import com.xpdustry.avs.service.providers.type.AddressProvider;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;


public class RestrictedModeConfig extends AbstractConfig {
  private static final RestrictedModeConfig INSTANCE = new RestrictedModeConfig();
  
  private RestrictedModeConfig() { super("restrict", true); }
  public static RestrictedModeConfig instance() { return INSTANCE; }

  @Override
  protected String configBundleKey(String key) {
    return "avs." + name + "." + key;
  }

  @Override
  protected String fieldDescBundleKey(IField<?> field) {
    return "avs." + name + ".field." + field.name();
  }
  
  @Override
  protected Fi getFile() {
    return AVSConfig.subDir(AVSConfig.settingsDirectory.getString()).child(name + ".json");
  }

  @Override
  protected void loadMisc() {
    com.xpdustry.avs.misc.JsonSerializer.apply(config.getJson());
    
    all.each(s -> s instanceof CachedField, s -> ((CachedField<?>) s).load());
  }
  
  public boolean save() {
    try { 
      all.each(s -> s instanceof CachedField, s -> ((CachedField<?>) s).save());
      logger.info(configBundleKey("saved"));
      return true;
    
    } catch (Exception e) {
      logger.info(configBundleKey("save-failed"), config.getFile().path());
      logger.err("avs.general-error", e.toString());
      return false;
    }
  }
  
  
  public static class Default {
    public static final boolean enabled = true;
    
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
        c instanceof ConfigCommand ||
        c instanceof ProviderCommand ||
        c instanceof InfoCommand ||
        c instanceof HelpCommand
    );
    
    public static final ObjectMap<AddressProvider, ProviderActionSeq> actions = providers.asMap(p -> p, 
        p -> new ProviderActionSeq(ProviderAction.getAll(ProviderAction.Category.cached)
                                                 .addAll(ProviderAction.add, ProviderAction.remove)));  
  }
  
  
  public static final Field<Boolean> enabled = 
      new Field<>(INSTANCE, "enabled", Default.enabled);
  public static final FieldList<AVSConfig.Field> settings =
      new FieldList<>(INSTANCE, "settings", AVSConfig.Field.class, Default.settings, ConfigEvents::onSettingsChanged);
  public static final FieldList<AddressProvider> providers = 
      new FieldList<>(INSTANCE, "providers", AddressProvider.class, Default.providers, ConfigEvents::onProvidersChanged);
  public static final FieldList<Command> commands =
      new FieldList<>(INSTANCE, "commands", Command.class, Default.commands, ConfigEvents::onCommandsChanged);
  public static final FieldMap<AddressProvider, ProviderActionSeq> actions = 
      new FieldMap<>(INSTANCE, "actions", AddressProvider.class, ProviderActionSeq.class, Default.actions, ConfigEvents::onActionsChanged);

}
