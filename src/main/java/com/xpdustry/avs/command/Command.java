package com.xpdustry.avs.command;

import com.xpdustry.avs.util.Logger;


public abstract class Command {
  public static final String keyPrefix = "avs.command.",
                             argsKeySuffix = ".args",
                             descKeySuffix = ".description",
                             helpKeySuffix = ".help";
  
  /** The command name. Will be used to get args, description and help page, in the bundle */
  public final String name;
  protected final Logger defaultLogger;
  
  
  public Command(String name) {
    this.name = name;
    this.defaultLogger = new Logger("Command/" + arc.util.Strings.capitalize(name));
  }
  
  public String getArgs() { return getArgs(defaultLogger); }
  public String getArgs(Logger logger) {
    return logger.getKey(keyPrefix + name + argsKeySuffix);
  }
  
  public String getDesc() { return getDesc(defaultLogger); }
  public String getDesc(Logger logger) {
    return logger.getKey(keyPrefix + name + descKeySuffix);
  }
  
  public String getHelp() { return getHelp(defaultLogger); }
  public String getHelp(Logger logger) {
    return logger.getKey(keyPrefix + name + helpKeySuffix);
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
