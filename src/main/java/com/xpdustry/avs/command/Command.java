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

package com.xpdustry.avs.command;

import com.xpdustry.avs.util.Logger;

import arc.util.Strings;


public abstract class Command {
  public static final String argsKeyFormat = "avs.command.@.args",
                             descKeyFormat = "avs.command.@.description",
                             helpKeyFormat = "avs.command.@.help";
  
  /** The command name. Will be used to get args, description and help page, in the bundle */
  public final String name;
  protected final Logger defaultLogger;
  
  
  public Command(String name) {
    this.name = name;
    this.defaultLogger = new Logger("Command/" + arc.util.Strings.capitalize(name));
  }
  
  public String getArgs() { return getArgs(defaultLogger); }
  public String getArgs(Logger logger) {
    return logger.getKey(Strings.format(argsKeyFormat, name));
  }
  
  public String getDesc() { return getDesc(defaultLogger); }
  public String getDesc(Logger logger) {
    return logger.getKey(Strings.format(descKeyFormat, name));
  }
  
  public String getHelp() { return getHelp(defaultLogger); }
  public String getHelp(Logger logger) {
    return logger.getKey(Strings.format(helpKeyFormat, name));
  }
 
  public void run(String[] args) { run(args, defaultLogger, false); };
  public void run(String[] args, Logger logger) { run(args, logger, false); };
  public abstract void run(String[] args, Logger logger, boolean restrictedMode);
  
  
  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || !(o instanceof Command)) return false;
    return ((Command) o).name.equals(name);
  }
}
