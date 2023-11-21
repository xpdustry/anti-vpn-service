package avs.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import arc.util.Log;
import arc.util.Log.LogLevel;


public class Logger{
  public static String pluginTopic = "&lc[Anti VPN Service]";
  
  public final String topic;
  
  public Logger() { this(""); }
  public Logger(String topic) {
    this.topic = topic;
  }
  
  public void log(LogLevel level, String text, Object... args){
    Log.log(level, String.join(" ", pluginTopic, topic + "&fr", text), args);
  }

  public void debug(String text, Object... args){
    log(LogLevel.debug, text, args);
  }
  
  public void debug(Object object){
    debug(String.valueOf(object));
  }
  
  public  void info(String text, Object... args){
    log(LogLevel.info, text, args);
  }
  
  public void info(Object object){
    info(String.valueOf(object));
  }
  
  public void warn(String text, Object... args){
    log(LogLevel.warn, text, args);
  }
  
  public void err(String text, Object... args){
    log(LogLevel.err, text, args);
  }
  
  public void err(Throwable th){
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    th.printStackTrace(pw);
    err(sw.toString());
  }
  
  public void err(String text, Throwable th){
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    th.printStackTrace(pw);
    err(text + ": " + sw);
  }
}
