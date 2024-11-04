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

package com.xpdustry.avs.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import arc.files.Fi;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.ReusableByteInStream;
import arc.util.io.Streams;
import arc.util.serialization.*;


/**
 * Implementation of {@link arc.Settings} to handle multiple settings files,
 * and without not wanted things, like settings backups.
 */
public class DynamicSettings {
  protected final static byte typeBool = 0, typeInt = 1, typeLong = 2, typeFloat = 3, typeString = 4, typeBinary = 5;
  protected static Seq<DynamicSettings> files = new Seq<>();
  protected static Logger logger = new Logger("Settings");
  protected static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
  
  public static Fi logFile = mindustry.Vars.modDirectory.child("settings.log");
  protected static int autosaveSpacing = 360; // in seconds
  protected static Thread autosaveThread;
  protected static String threadName = "Settings-Autosave";
  
  protected final Fi file;
  
  protected ObjectMap<String, Object> values = new ObjectMap<>();
  protected boolean modified;
  protected boolean shouldAutosave = true, logging = true;
  /** Option to store settings in a simple or a binary json */
  protected final boolean simpleJson;

  //IO utility objects
  protected ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
  protected ReusableByteInStream byteInputStream = new ReusableByteInStream();
  protected BaseJsonReader reader;
  protected Json json;
  
  public DynamicSettings(Fi file) { this(file, false); }
  public DynamicSettings(Fi file, boolean simpleJson) {
    this.file = file;
    this.simpleJson = simpleJson;
    this.reader = simpleJson ? new JsonReader() : new UBJsonReader();
    
    setJson(new Json());
    files.add(this);
  }

  public void setJson(Json json){
      this.json = json;
      if (simpleJson) this.json.setOutputType(JsonWriter.OutputType.json);
  }

  /** 
   * Set whether the data should autosave immediately upon changing a value.
   * Default value: true. 
   */
  public void setAutosave(boolean autosave){
      this.shouldAutosave = autosave;
  }

  public void setLogging(boolean logging) {
    this.logging = logging;
  }
  
  public boolean modified(){
      return modified;
  }

  /** Loads all values. */
  public synchronized void load(){
      //doesn't load the file and create an empty one
      if(!getFile().exists()){
          writeLog("File not found");
          save();
          return;
      }
    
      try{
          loadValues(getFile());
      }catch(Throwable error){
          writeLog("Load error: " + Strings.getStackTrace(error));
          throw new RuntimeException(error);
      }
  }

  /** Saves all values. */
  public synchronized void save(){
      try{
          saveValues(getFile());
      }catch(Throwable error){
          writeLog("Save error: " + Strings.getStackTrace(error));
          throw new RuntimeException(error);
      }
      modified = false;
  }

  /** Saves if any modifications were done. */
  public synchronized void autosave(){
      if(modified && shouldAutosave) save();
  }
  
  /** Static part, for autosave **/
  
  public synchronized static boolean needGlobalSave() {
    return files.contains(f -> f.modified);
  }
  
  public synchronized static void globalAutosave() {
    // Only save if changes are made
    if (needGlobalSave()) {
      writeLogStatic("Global autosave...");
      logger.info("avs.settings.autosave.started");
      files.each(s -> s.autosave());
      logger.info("avs.settings.autosave.finished");      
    }
  }
  
  public static boolean stopAutosave() {
    if (autosaveThread != null) {
      autosaveThread.interrupt();
      try { autosaveThread.join(1000);  } 
      catch (InterruptedException ignored) {}
      autosaveThread = null;
      return true;
    }
    return false;
  }
  
  public static boolean startAutosave() { return startAutosave(null); }
  public static boolean startAutosave(String threadName) {
    if (autosaveThread == null) {
      if (threadName == null || threadName.isBlank())
           threadName = DynamicSettings.threadName;
      else DynamicSettings.threadName = threadName;
        
      autosaveThread = arc.util.Threads.daemon(threadName, () -> {
        writeLogStatic("Autosave thread started!");
        logger.info("avs.settings.autosave.thread.started");
        
        while (true) {
          try { Thread.sleep(autosaveSpacing * 1000); } 
          catch (InterruptedException e) { 
            writeLogStatic("Autosave thread stopped!");
            logger.info("avs.settings.autosave.thread.stopped");
            return; 
          }; 
          
          globalAutosave(); 
        }
      });
      return true;
    }
    return false;
  }
  
  public static boolean isAutosaveRunning() {
    return autosaveThread != null && autosaveThread.isAlive();
  }
  
  public static int autosaveSpacing() {
    return autosaveSpacing;
  }
  
  public static void setAutosaveSpacing(int spacing) {
    if (spacing < 1) throw new IllegalArgumentException("spacing must be greater than 1 second");
    autosaveSpacing = spacing;
  }
  
  /**********************/

  public synchronized void loadValues(Fi file) throws IOException{
      try(DataInputStream stream = new DataInputStream(file.read(8192))){
            if (simpleJson) {
              JsonValue content = reader.parse(stream);
              
              if (content != null) {
                 for(JsonValue child = content.child; child != null; child = child.next){
                      if (child.isBoolean()) values.put(child.name, child.asBoolean());
                      else if (child.isLong()) values.put(child.name, child.asLong());
                      else if (child.isDouble()) values.put(child.name, child.asDouble());
                      else if (child.isString()) values.put(child.name, child.asString());
                      else if (child.isNull()) values.put(child.name, null);
                      // Check if it's a "bytes json object" (explains in {@link #saveValues()})
                      else if (child.isObject() && child.child != null && child.child.next == null &&
                               child.child.name != null && child.child.name.equals("bytes") && child.child.isString())
                          values.put(child.name, Base64Coder.decode(child.child.asString()));
                      // All sub-objects or arrays, are count as json values.
                      else values.put(child.name, child);
                  }                
              }

          } else {
              int amount = stream.readInt();
              
              /**
               * I remove this because when we saving with no values, like when {@link #load()} is called and
               * the file doesn't exists, it create an empty one, this error can be throws after reloaded the file.
               * 
               * //current theory: when corruptions happen, the only things written to the stream are a bunch of zeroes
               * //try to anticipate this case and throw an exception when 0 values are written
               * if(amount <= 0) throw new IOException("0 values are not allowed. "
               *                                     + "The file is probably corrupted, please remove it.");
               */
             
              for(int i = 0; i < amount; i++){
                  String key = stream.readUTF();
                  byte type = stream.readByte();
    
                  switch(type){
                      case typeBool:
                          values.put(key, stream.readBoolean());
                          break;
                      case typeInt:
                          values.put(key, stream.readInt());
                          break;
                      case typeLong:
                          values.put(key, stream.readLong());
                          break;
                      case typeFloat:
                          values.put(key, stream.readFloat());
                          break;
                      case typeString:
                          values.put(key, stream.readUTF());
                          break;
                      case typeBinary:
                          int length = stream.readInt();
                          byte[] bytes = new byte[length];
                          stream.read(bytes);
                          values.put(key, bytes);
                          break;
                      default:
                          throw new IOException("Unknown key type: " + type);
                  }
              }
              //make sure all data was read - this helps with potential corruption
              int end = stream.read();
              if(end != -1){
                  throw new IOException("Trailing settings data; expected EOF, but got: " + end);
              }         
          }

          Streams.close(stream);
      }
      
      writeLog("Loaded " + values.size + " values");
  }

  public synchronized void saveValues(Fi file) throws IOException{
      try(DataOutputStream stream = new DataOutputStream(file.write(false, 8192))){
          if (simpleJson) {
            JsonValue content = new JsonValue(JsonValue.ValueType.object);
            
            for(ObjectMap.Entry<String, Object> entry : values.entries()){
              Object value = entry.value;
              JsonValue jvalue = null;

              if (value instanceof Boolean) jvalue = new JsonValue((Boolean) value);
              else if (value instanceof Integer) jvalue = new JsonValue((Integer) value);
              else if (value instanceof Long) jvalue = new JsonValue((Long) value);
              else if (value instanceof Float) jvalue = new JsonValue((Float) value);
              //else if (value instanceof Double) jvalue = new JsonValue((Double) value);
              else if (value instanceof String) jvalue = new JsonValue((String) value);
              else if (value instanceof byte[]) {
                /** 
                 * There is no byte array in json, so it will be converted to a json object with a 
                 * key "bytes" and base64 coded value.
                 */
                jvalue = new JsonValue(JsonValue.ValueType.object);
                jvalue.addChild("bytes", new JsonValue(new String(Base64Coder.encode((byte[]) value))));
              }
              else if (value instanceof JsonValue) jvalue = (JsonValue) value;
              
              if (jvalue != null) content.addChild(entry.key, jvalue);
            }
            
            OutputStreamWriter out = new OutputStreamWriter(stream);
            Strings.jsonPrettyPrint(content, out, JsonWriter.OutputType.json);
            out.flush();
            
          } else {
              stream.writeInt(values.size);
    
              for(ObjectMap.Entry<String, Object> entry : values.entries()){
                  stream.writeUTF(entry.key);
    
                  Object value = entry.value;
    
                  if(value instanceof Boolean){
                      stream.writeByte(typeBool);
                      stream.writeBoolean((Boolean)value);
                  }else if(value instanceof Integer){
                      stream.writeByte(typeInt);
                      stream.writeInt((Integer)value);
                  }else if(value instanceof Long){
                      stream.writeByte(typeLong);
                      stream.writeLong((Long)value);
                  }else if(value instanceof Float){
                      stream.writeByte(typeFloat);
                      stream.writeFloat((Float)value);
                  }else if(value instanceof String){
                      stream.writeByte(typeString);
                      stream.writeUTF((String)value);
                  }else if(value instanceof byte[]){
                      stream.writeByte(typeBinary);
                      stream.writeInt(((byte[])value).length);
                      stream.write((byte[])value);
                  }
              }
              
              stream.flush();
          }
        
          Streams.close(stream);

      }catch(Throwable e){
          writeLog("Error while writing values; The file is corrupt");
          throw new IOException("Error while writing file: " + file, e);
      }

      writeLog("Saved " + values.size + " values; " + file.length() + " bytes");
  }

  /** Return whether the file exists or not. */
  public boolean fileExists() {
    return file.exists();
  }
  
  /** Returns the file used for writing settings to. */
  public Fi getFile(){
      return file;
  }

  /** Clears all preference values. */
  public synchronized void clear(){
      values.clear();
      modified = true;
  }

  public synchronized boolean has(String name){
      return values.containsKey(name);
  }
  
  public synchronized Object get(String name, Object def){
      if (!values.containsKey(name)) return def;
      Object o = values.get(name);
      
      // Because simple json only store long and double.
      if (simpleJson) {
        if (o instanceof Double) o = (float) ((double) o);
        else if (o instanceof Long) o = (int) ((long) o);
      }
      
      return o;
  }
  
  /** Same as {@link #get(String, Object)}, but put {@code def} if the key is not found */
  public synchronized Object getOrPut(String name, Object def){
      Object o = get(name, def);
      if (o == def) put(name, def);
      return o;
  }


  public boolean isModified(){
      return modified;
  }

  public synchronized void putJson(String name, Object value){
      putJson(name, null, value);
  }

  public synchronized void putJson(String name, Class<?> elementType, Object value){
      try {
          if (simpleJson) {
              JsonBuilder builder = new JsonBuilder();
    
              json.setWriter(builder);
              json.writeValue(value, value == null ? null : value.getClass(), elementType);
    
              put(name, builder.getJson());
          
          } else {
              byteStream.reset();
              
              json.setWriter(new UBJsonWriter(byteStream));
              json.writeValue(value, value == null ? null : value.getClass(), elementType);  
              
              put(name, byteStream.toByteArray());
          }
    
          modified = true;  
          
      } catch(Throwable e) {
          writeLog("Failed to put JSON key=" + name + ":\n" + Strings.getStackTrace(e));
          throw new RuntimeException(e);
      }
  }

  /**
   * @apiNote if the key is not found, {@code def} is put and returned.
   */
  @SuppressWarnings("rawtypes")
  public synchronized <T> T getJson(String name, Class<T> type, Class elementType, Prov<T> def){
      if(!has(name)) {
          // put and return the default value
          T fall = def.get();
          putJson(name, elementType, fall);
          return fall;
      }
    
      try{
          JsonValue jvalue;
          if (simpleJson) jvalue = (JsonValue) get(name, null);
          else {
            byteInputStream.setBytes(getBytes(name));
            jvalue = reader.parse(byteInputStream);
          }
          
          if (jvalue == null) return def.get();
          
          T decoded = json.readValue(type, elementType, jvalue);
          // if null, then the json was not decoded correctly 
          if (decoded == null) throw new IllegalStateException("json value is null");
          return decoded;
          
      }catch(Throwable e){
          writeLog("Failed to read JSON key=" + name + " type=" + type + ":\n" + Strings.getStackTrace(e));
          throw new RuntimeException(e);
      }
  }

  public <T> T getJson(String name, Class<T> type, Prov<T> def){
      return getJson(name, type, null, def);
  }

  public float getFloat(String name, float def){
      return (float)getOrPut(name, def);
  }

  public int getInt(String name, int def){
      return (int)getOrPut(name, def);
  }

  public boolean getBool(String name, boolean def){
      return (boolean)getOrPut(name, def);
  }

  public byte[] getBytes(String name, byte[] def){
      return (byte[])getOrPut(name, def);
  }

  public String getString(String name, String def){
      return (String)getOrPut(name, def);
  }

  public float getFloat(String name){
      return (float)get(name, 0f);
  }
  
  public Long getLong(String name){
      return (long)get(name, 0);
  }

  public int getInt(String name){
      return (int)get(name, 0);
  }
 
  public boolean getBool(String name){
      return (boolean)get(name, false);
  }

  /** Runs the specified code once, and never again. */
  public void getBoolOnce(String name, Runnable run){
      if(!getBool(name, false)){
          run.run();
          put(name, true);
      }
  }

  /** Returns true once, and never again. */
  public boolean getBoolOnce(String name){
      boolean val = getBool(name, false);
      put(name, true);
      return val;
  }

  public byte[] getBytes(String name){
      return (byte[])get(name, null);
  }

  public String getString(String name){
      return (String)get(name, null);
  }

  public void putAll(ObjectMap<String, Object> map){
      map.each((k, v) -> put(k, v));
  }

  /** Stores an object in the preference map. */
  public synchronized void put(String name, Object object){
      if(object instanceof Float || object instanceof Integer || object instanceof Boolean ||
         object instanceof String || object instanceof byte[] || (simpleJson && object instanceof JsonValue)){
          values.put(name, object);
          modified = true;
      }else{
          throw new IllegalArgumentException("Invalid object stored: " + (object == null ? null : object.getClass()) + ".");
      }
  }

  public synchronized void remove(String name){
      values.remove(name);
      modified = true;
  }

  public synchronized Iterable<String> keys(){
      return values.keys();
  }

  public synchronized int keySize(){
      return values.size;
  }

  /** Appends to the settings log. Used for diagnosis of the save wipe bug. Never throws an error. */
  protected void writeLog(String text){
    if (!logging) return; 
    try{
        logFile.writeString("[" + dateFormat.format(new Date()) + "] {" + file.path() + "} " + text + "\n", true);
    }catch(Throwable t){
        logger.err("avs.settings.error.file", t, file.path(), logFile.path());
    }
  }
  
  protected static void writeLogStatic(String text){
    try{
        logFile.writeString("[" + dateFormat.format(new Date()) + "] " + text + "\n", true);
    }catch(Throwable t){
        logger.err("avs.settings.error.static", t, logFile.path());
    }
  }
  
}
