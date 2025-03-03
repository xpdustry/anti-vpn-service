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

package com.xpdustry.avs.command.list;

import com.xpdustry.avs.command.AVSCommandManager;
import com.xpdustry.avs.command.Command;
import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.config.RestrictedModeConfig;
import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.providers.ProviderAction;
import com.xpdustry.avs.service.providers.type.AddressProvider;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.config.*;
import com.xpdustry.avs.util.logging.Logger;


public class RestrictCommand extends Command {
  public RestrictCommand() { super("restrict"); }

  @Override
  public void run(String[] args, Logger logger, boolean restrictedMode) {
    if (args.length == 0) {
      String valueF = logger.getKey("avs.command.restrict.value");
      String valuepF = logger.getKey("avs.command.restrict.value-plus");
      String descF =  logger.getKey("avs.command.restrict.desc");
      String nextF = logger.getKey("avs.command.restrict.next");
      
      StringBuilder builder = new StringBuilder();
      RestrictedModeConfig.instance().all.each(f -> {
        String[] lines = f.toString(true).split("\n");
        builder.append(Strings.format(valueF, f.name, lines[0])).append('\n');
        for (int i=1; i<lines.length; i++)
          builder.append(Strings.format(valuepF, lines[i])).append('\n');
        for (String line : f.desc(logger).split("\n"))
          builder.append(Strings.format(descF, line)).append('\n');
        logger.infoNormal(builder.toString() + nextF);
        builder.setLength(0);
      });  
      return;
    }
    
    if (args[0].equals("reload")) {
      RestrictedModeConfig.instance().reload();
      return;
    }
    
    Field<?> field = RestrictedModeConfig.instance().get(args[0]);
    
    if (field == null) {
      logger.err("avs.command.restrict.field.not-found", args[0]);

    } else if (args.length == 1) {
      StringBuilder builder = new StringBuilder();
      String[] lines = logger.formatKey("avs.command.restrict.field.value.is", field.name, field.toString(true))
                             .split("\n");
      builder.append(lines[0]).append('\n');
      for (int i=1; i<lines.length; i++) 
        builder.append(logger.formatKey("avs.command.restrict.field.value.is.line", lines[i])).append('\n');
      lines = logger.formatKey("avs.command.restrict.field.value.is.default", field.defaultToString(true))
                    .split("\n");
      builder.append('\n').append(lines[0]).append('\n');
      for (int i=1; i<lines.length; i++) 
        builder.append(logger.formatKey("avs.command.restrict.field.value.is.line", lines[i])).append('\n');
      logger.infoNormal(builder.toString());

    } else if (args[1].equals("default")) {
      if (field.setDefault(logger)) logger.info("avs.command.restrict.field.value.default", field.name);
 
    //Too lazy to make a real modular interface  
    } else if (field == RestrictedModeConfig.enabled) {
      String value = String.join(" ", arc.util.Structs.remove(args, 0));
      if (Strings.isTrue(value)) {
        if (RestrictedModeConfig.enabled.set(true, logger)) logger.info("avs.restrict.enabled.on");
      } else if (Strings.isFalse(value)) {
        if (RestrictedModeConfig.enabled.set(false, logger)) logger.info("avs.restrict.enabled.off");
      } else logger.info("avs.command.restrict.field.value.invalid", value, field.name, field.type.getSimpleName());
      
    } else if (field == RestrictedModeConfig.settings) {
      runFieldList(args, logger, RestrictedModeConfig.settings, s -> (AVSConfig.ConfigField)AVSConfig.instance().get(s),
                   "avs.command.config.field.not-found");
      
    } else if (field == RestrictedModeConfig.providers) {
      runFieldList(args, logger, RestrictedModeConfig.providers, AntiVpnService::get, 
                   "avs.command.provider.not-found");
      
    } else if (field == RestrictedModeConfig.commands) {
      runFieldList(args, logger, RestrictedModeConfig.commands, AVSCommandManager::get, 
                   "avs.command.not-found2");
      
    } else if (field == RestrictedModeConfig.actions) {
      String arg;
      AddressProvider provider;
      ProviderAction action;
      
      switch (args[1]) {
        case "add":
          if (args.length < 4) {
            logger.err("avs.command.restrict.missing-argument");
            return;
          }
          provider = AntiVpnService.get(args[2]);
          arg = Strings.join(" ", args, 3, args.length);
          action = ProviderAction.get(arg);
          
          if (provider == null) logger.err("avs.command.provider.not-found", args[2]);
          else if (action == null) logger.err("avs.command.provider.action.not-found", arg);
          else if (RestrictedModeConfig.actions.contains(provider, action))
            logger.err("avs.command.restrict.field.value.contains", field.name);
          else if (RestrictedModeConfig.actions.add(provider, action, logger)) 
            logger.info("avs.command.restrict.field.value.add", field.name);
          return;
          
        case "del":
          if (args.length < 3) logger.err("avs.command.restrict.missing-argument");
          else if (args.length < 4) {
            provider = AntiVpnService.get(args[2]);
            
            if (provider == null) logger.err("avs.command.provider.not-found", args[2]);
            else if (!RestrictedModeConfig.actions.contains(provider))
              logger.err("avs.command.restrict.field.value.not-contains-key", field.name);
            else if (RestrictedModeConfig.actions.remove(provider, logger))
              logger.info("avs.command.restrict.field.value.del-key", field.name);
            
          } else {
            provider = AntiVpnService.get(args[2]);
            arg = Strings.join(" ", args, 3, args.length);
            action = ProviderAction.get(arg);
            
            if (provider == null) logger.err("avs.command.provider.not-found", args[2]);
            else if (action == null) logger.err("avs.command.provider.action.not-found", arg);
            else if (!RestrictedModeConfig.actions.contains(provider, action))
              logger.err("avs.command.restrict.field.value.not-contains", field.name);
            else if (RestrictedModeConfig.actions.remove(provider, action, logger))
              logger.info("avs.command.restrict.field.value.del", field.name);
            
          }
          return;
          
        case "clear":
          if (args.length > 2) {
            arg = Strings.join(" ", args, 2, args.length);
            provider = AntiVpnService.get(arg);
            
            if (provider == null)  logger.err("avs.command.provider.not-found", arg);
            else if (!RestrictedModeConfig.actions.contains(provider))
              logger.err("avs.command.restrict.field.value.not-contains-key", field.name);
            else if (RestrictedModeConfig.actions.clear(provider))
              logger.info("avs.command.restrict.field.clean.key", field.name, provider.name);
            
          } else {
            RestrictedModeConfig.actions.clear();
            logger.info("avs.command.restrict.field.clean", field.name);
          }
          return;
          
        default:
          logger.info("avs.command.restrict.invalid-argument");
      }
    }
  }
  
  private static <T> void runFieldList(String[] args, Logger logger, FieldList<T> field, 
                                       arc.func.Func<String, T> getter, String notFoundKey) {
    String arg = Strings.join(" ", args, 2, args.length);
    boolean hasArgs = arg != null && !arg.isBlank();
    T value = hasArgs ? getter.get(arg) : null;
    
    switch (args[1]) {
      case "add":
        if (value == null) break;
        else if (field.contains(value)) logger.err("avs.command.restrict.field.value.contains", field.name);
        else if (field.add(value, logger))  logger.info("avs.command.restrict.field.value.add", field.name);
        return;
        
      case "del":
        if (value == null) break;
        else if (!field.contains(value)) logger.err("avs.command.restrict.field.value.not-contains", field.name);
        else if (field.remove(value, logger)) logger.info("avs.command.restrict.field.value.del", field.name);
        return;
        
      case "clear":
        field.clear();
        logger.info("avs.command.restrict.field.clean", field.name);
        return;
        
      default:
        logger.err("avs.command.restrict.invalid-argument");
        return;
    }
    
    if (!hasArgs) logger.err("avs.command.restrict.missing-argument");
    else logger.err(notFoundKey, arg);
  }
}
