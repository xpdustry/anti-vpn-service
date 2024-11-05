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

package com.xpdustry.avs.command.list;

import com.xpdustry.avs.config.RestrictedModeConfig;
import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.providers.ProviderAction;
import com.xpdustry.avs.service.providers.type.AddressProvider;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.Strings;

import arc.struct.Seq;


public class ProviderCommand extends com.xpdustry.avs.command.Command {
  public ProviderCommand() { super("provider"); }

  @Override
  public void run(String[] args, Logger logger, boolean restrictedMode) {
    if (args.length == 0) {
      Seq<AddressProvider> list = restrictedMode ? RestrictedModeConfig.providers : 
                                                   AntiVpnService.allProviders;

      if (list.isEmpty()) {
        logger.warn("avs.command.provider.nothing");
        return;
      }
      
      String format = logger.getKey("avs.command.provider.format");
      StringBuilder builder = new StringBuilder();
      
      builder.append(logger.getKey("avs.command.provider.availables")).append('\n');
      list.each(p -> 
          builder.append(Strings.format(format, p.name, p.displayName)).append('\n'));
      logger.infoNormal(builder.toString());
      return;
    }
    
    if (args[0].equals("actions")) {
      String format = logger.getKey("avs.command.provider.action.format");
      StringBuilder builder = new StringBuilder();
      
      ProviderAction.Category.all.each(c -> {
        builder.append(c.getDesc(logger)).append('\n');
        addProviderActions(builder, format, ProviderAction.getAll(c), logger);
        logger.infoNormal(builder.append(' ').toString());
        builder.setLength(0);
      });      
      return;
    }
    
    AddressProvider provider = AntiVpnService.allProviders.find(c -> c.name.equals(args[0]));
    
    if (provider == null) {
      logger.err("avs.command.provider.not-found", args[0]);
      return;
    } else if (restrictedMode && !RestrictedModeConfig.providers.contains(provider)) {
      logger.err("avs.command.provider.restricted");
      return;
    } else if (args.length == 1) {
      String format = logger.getKey("avs.command.provider.action.format");
      StringBuilder builder = new StringBuilder();
      
      builder.append(logger.getKey("avs.command.provider.action.availables")).append('\n');
      addProviderActions(builder, format, 
          restrictedMode && !provider.actions.isEmpty() ? provider.actions : 
                                                          ProviderAction.getAll(provider), 
          logger);
      logger.infoNormal(builder.toString());
      return;
    }
    
    ProviderAction action = ProviderAction.get(args[1]);
    
    if (action == null) {
      logger.err("avs.command.provider.action.not-found", args[1]);
      return;
    } else if (!ProviderAction.Category.getAll(provider).contains(c -> c == action.category)) {
      logger.err("avs.command.provider.action.not-compatible");
      return;
    } else if (restrictedMode && !provider.actions.isEmpty() && !provider.actions.contains(action)) {
      logger.err("avs.command.provider.action.restricted");
      return;
    }
    
    if (action.argRequired()) {
      if (args.length < 3) {
        logger.err("avs.command.provider.action.arg-required");
        return;
      }
      
      String[] rest = new String[args.length-2];
      System.arraycopy(args, 2, rest, 0, rest.length);
      String value = String.join(" ", rest);
      
      action.run(provider, value, logger);
    } else action.run(provider, logger);
  }
  
  private static void addProviderActions(StringBuilder builder, String format,
                                         Seq<ProviderAction> actions,  Logger logger) {
    Seq<String> args = actions.map(a -> a.getArgs(logger));
    
    int best = Strings.best(actions, a -> a.name.length());
    
    for (int i=0; i<actions.size; i++) {
      String text;
      
      if (args.get(i).isEmpty())
        text = Strings.format(format, Strings.lJust(actions.get(i).name, best), "", 
                                      actions.get(i).getDesc(logger));
      else
        text = Strings.format(format, actions.get(i).name, args.get(i)+" ", 
                                      actions.get(i).getDesc(logger));
      
      builder.append(text).append('\n');
    }
  }
}
