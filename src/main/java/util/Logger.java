package util;

import arc.util.Log;

public class Logger extends Log {
  public static void log(Log.LogLevel level, String text, Object... args){
    Log.log(level, "&lc&fi[Anti VPN Service]&fr " + text, args);
  }
}
