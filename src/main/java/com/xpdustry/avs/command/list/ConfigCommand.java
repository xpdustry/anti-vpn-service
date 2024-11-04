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

import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.config.ConfigField;
import com.xpdustry.avs.config.RestrictedModeConfig;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.Strings;

import arc.struct.Seq;


public class ConfigCommand extends com.xpdustry.avs.command.Command {
  public ConfigCommand() { super("config"); }

  @Override
  public void run(String[] args, Logger logger, boolean restrictedMode) {
    if (args.length == 0) {
      printSettings(restrictedMode ? RestrictedModeConfig.settings : AVSConfig.all, logger);
      return;
    }

    if (args[0].equals("reload")) {
      AVSConfig.load();
      AVSConfig.notifyAllValueChanged();
      logger.none();
      logger.info("avs.command.config.reloaded");
      return;
    }
    
    ConfigField field = AVSConfig.get(args[0]);
    
    if (field == null) {
      logger.err("avs.command.config.field.not-found", args[0]);
      return;
    } else if (restrictedMode && !RestrictedModeConfig.settings.contains(field)) {
      logger.err("avs.command.config.field.restricted");
      return;
    } else if (args.length == 1) {
      logger.info("avs.command.config.field.value.is", field.name, 
                  Strings.objToStr(field.get()), Strings.objToStr(field.defaultValue));
      return;
    } else if (field.readOnly && args.length > 1) {
      logger.err("avs.command.config.field.read-only", field.name);
      return;
    }
    
    String value = String.join(" ", arc.util.Structs.remove(args, 0));
    Object v = null;
    boolean positive = false; // in most cases, int and float value are always positives
    
    if (field.isBool()) {
      if (Strings.isTrue(value)) v = true;
      else if (Strings.isFalse(value)) v = false;
    } else if (field.isInt()) {
      int i = Strings.parseInt(value);
      if (i != Integer.MIN_VALUE) {
        if (i < 0) positive = true;
        else v = i;
      }
    } else if (field.isFloat()) {
      float d = Strings.parseFloat(value);
      if (d != Float.MIN_VALUE) {
        if (d < 0) positive = true;
        else v = d;
      }
    } else if (field.isString()) {
      if (value.equals("\"\"")) v = "";
      else v = value;
    } else logger.errNormal("ho no! invalid configuration!"); // what?
    
    if (v == null) {
      logger.err("avs.command.config.field.value.invalid", value, field.name, 
                 (positive ? "Positive " : "") + field.defaultValue.getClass().getSimpleName());
      return;  
      
    }
    
    if (field.set(v, logger)) logger.info("avs.command.config.field.value.set", field.name, v);
  }
  
  private static void printSettings(Seq<ConfigField> list, Logger logger) {
    if (list.isEmpty()) {
      logger.warn("avs.command.config.nothing");
      return;
    }
    
    String valueF = logger.getKey("avs.command.config.value");
    String descF =  logger.getKey("avs.command.config.desc");
    String nextF = logger.getKey("avs.command.config.next");
    
    Seq<ConfigField> dev = list.select(f -> f.isDev);
    list = list.select(f -> !f.isDev);
    
    StringBuilder builder = new StringBuilder();
    arc.func.Cons<Seq<ConfigField>> printer = (l) -> {
      l.each(f -> {
        builder.append(Strings.format(valueF, f.name, Strings.objToStr(f.get()))).append('\n');
        for (String line : f.getDescription(logger).split("\n"))
          builder.append(Strings.format(descF, line)).append('\n');
        logger.infoNormal(builder.toString() + nextF);
        builder.setLength(0);
      });
    };    
    
    if (!list.isEmpty()) {
      builder.append(logger.getKey("avs.command.config.normal-fields")).append('\n');
      printer.get(list);
    }
    
    if (!dev.isEmpty()) {
      builder.append('\n').append(logger.getKey("avs.command.config.dev-fields")).append('\n');
      printer.get(dev);
    }    
  }
}
