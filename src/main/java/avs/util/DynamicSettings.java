package avs.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import arc.files.Fi;
import arc.func.Cons;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.ObjectMap.Entry;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Threads;
import arc.util.io.ReusableByteInStream;
import arc.util.serialization.*;


  /*
   * Re-implement the arc.Settings to handle multiple settings files,
   * and remove not wanted things, like settings backups.
   * 
   * Will be used, for example, by local provider, to store his cache.
   */

public class DynamicSettings {
  protected final static byte typeBool = 0, typeInt = 1, typeLong = 2, typeFloat = 3, typeString = 4, typeBinary = 5;
  protected static Seq<DynamicSettings> files = new Seq<>();
  
  public static Fi logFile = mindustry.Vars.modDirectory.child("settings.log");
  public static int autosaveTimeout = 360; // in seconds
  public static Thread autosaveThread = Threads.daemon("AVS-Autosave", () -> {
    try { Threads.sleep(autosaveTimeout * 1000); }
    catch (RuntimeException e) { return; };
    writeLogStatic("Autosave...");
    files.each(s -> s.autosave());
  });
  
  protected final Fi file;
  
  protected HashMap<String, Object> values = new HashMap<>();
  protected boolean modified;
  protected Cons<Throwable> errorHandler = Log::err;
  protected boolean hasErrored = false, 
                    shouldAutosave = true, 
                    loaded = false, 
                    logging = true;
  protected final boolean simpleJson; // If store settings in a simple or binary json

  //IO utility objects
  protected ByteArrayOutputStream byteStream = new ByteArrayOutputStream(32);
  protected ReusableByteInStream byteInputStream = new ReusableByteInStream();
  protected BaseJsonReader ureader;
  protected Json json = new Json();
  
  public DynamicSettings(Fi file) { this(file, false); }
  public DynamicSettings(Fi file, boolean simpleJson) {
    if (simpleJson) throw new UnsupportedOperationException("simpleJson is not supported for moment");
    this.file = file;
    this.simpleJson = simpleJson;
    this.ureader = new UBJsonReader();
    files.add(this);
  }

  public void setJson(Json json){
      this.json = json;
  }

  /**Sets the error handler function.
   * This function gets called when {@link #forceSave} or {@link #load} fails. This can occur most often on browsers,
   * where extensions can block writing to local storage.*/
  public void setErrorHandler(Cons<Throwable> handler){
      errorHandler = handler;
  }

  /** Set whether the data should autosave immediately upon changing a value.
   * Default value: true. */
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
      try{
          loadValues();
      }catch(Throwable error){
          writeLog("Error in load: " + Strings.getStackTrace(error));
          if(errorHandler != null){
              if(!hasErrored) errorHandler.get(error);
          }else{
              throw error;
          }
          hasErrored = true;
      }
      //if loading failed, it still counts
      loaded = true;
  }

  /** Saves all values. */
  public synchronized void forceSave(){
      //never loaded, nothing to save
      if(!loaded) return;
      try{
          saveValues();
      }catch(Throwable error){
          writeLog("Error in forceSave to:\n" + Strings.getStackTrace(error));
          if(errorHandler != null){
              if(!hasErrored) errorHandler.get(error);
          }else{
              throw error;
          }
          hasErrored = true;
      }
      modified = false;
  }

  /** Manually save, if the settings have been loaded at some point. */
  public synchronized void manualSave(){
      if(loaded){
          forceSave();
      }
  }

  /** Saves if any modifications were done. */
  public synchronized void autosave(){
      if(modified && shouldAutosave){
          forceSave();
          modified = false;
          writeLog("autosave");
      }
  }
  
  public synchronized static void forceGlobalAutosave() {
    writeLogStatic("Forcing autosave...");
    files.each(s -> s.autosave());
  }

  /** Loads a settings file into {@link #values} using the specified appName. */
  public synchronized void loadValues(){
      //don't load settings files if neither of them exist
      if(!getSettingsFile().exists()){
          writeLog("No settings file found");
          saveValues();
          return;
      }

      try{
          loadValues(getSettingsFile());
          writeLog("Loaded " + values.size() + " values");
      }catch(Throwable e){
          Log.err("Failed to load settings file.", e);
          writeLog("Failed to load file:\n" + Strings.getStackTrace(e));
      }
  }

  public synchronized void loadValues(Fi file) throws IOException{
      try(DataInputStream stream = new DataInputStream(file.read(8192))){
          int amount = stream.readInt();
          //current theory: when corruptions happen, the only things written to the stream are a bunch of zeroes
          //try to anticipate this case and throw an exception when 0 values are written
          if(amount <= 0) throw new IOException("0 values are not allowed.");
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
  }

  /** Saves all entries from {@link #values} into the correct location. */
  public synchronized void saveValues(){
      Fi file = getSettingsFile();

      try(DataOutputStream stream = new DataOutputStream(file.write(false, 8192))){
          stream.writeInt(values.size());

          for(Map.Entry<String, Object> entry : values.entrySet()){
              stream.writeUTF(entry.getKey());

              Object value = entry.getValue();

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

      }catch(Throwable e){
          //file is now corrupt, delete it
          file.delete();
          throw new RuntimeException("Error writing preferences: " + file, e);
      }

      writeLog("Saving " + values.size() + " values; " + file.length() + " bytes");
  }

  /** Returns the file used for writing settings to. Not available on all platforms! */
  public Fi getSettingsFile(){
      return file;
  }

  /** Clears all preference values. */
  public synchronized void clear(){
      values.clear();
  }

  public synchronized boolean has(String name){
      return values.containsKey(name);
  }

  public synchronized Object get(String name, Object def){
      return values.containsKey(name) ? values.get(name) : def;
  }

  public boolean isModified(){
      return modified;
  }

  public synchronized void putJson(String name, Object value){
      putJson(name, null, value);
  }

  public synchronized void putJson(String name, Class<?> elementType, Object value){
      byteStream.reset();

      json.setWriter(new UBJsonWriter(byteStream));
      json.writeValue(value, value == null ? null : value.getClass(), elementType);

      put(name, byteStream.toByteArray());

      modified = true;
  }

  @SuppressWarnings("rawtypes")
  public synchronized <T> T getJson(String name, Class<T> type, Class elementType, Prov<T> def){
      try{
          if(!has(name)) return def.get();
          byteInputStream.setBytes(getBytes(name));
          return json.readValue(type, elementType, ureader.parse(byteInputStream));
      }catch(Throwable e){
          writeLog("Failed to write JSON key=" + name + " type=" + type + ":\n" + Strings.getStackTrace(e));
          return def.get();
      }
  }

  public <T> T getJson(String name, Class<T> type, Prov<T> def){
      return getJson(name, type, null, def);
  }

  public float getFloat(String name, float def){
      return (float)get(name, def);
  }

  public long getLong(String name, long def){
      return (long)get(name, def);
  }

  public Long getLong(String name){
      return getLong(name, 0);
  }

  public int getInt(String name, int def){
      return (int)get(name, def);
  }

  public boolean getBool(String name, boolean def){
      return (boolean)get(name, def);
  }

  public byte[] getBytes(String name, byte[] def){
      return (byte[])get(name, def);
  }

  public String getString(String name, String def){
      return (String)get(name, def);
  }

  public float getFloat(String name){
      return getFloat(name, 0f);
  }

  public int getInt(String name){
      return getInt(name, 0);
  }

  public boolean getBool(String name){
      return getBool(name, false);
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
      return getBytes(name, null);
  }

  public String getString(String name){
      return getString(name, null);
  }

  public void putAll(ObjectMap<String, Object> map){
      for(Entry<String, Object> entry : map.entries()){
          put(entry.key, entry.value);
      }
  }

  /** Stores an object in the preference map. */
  public synchronized void put(String name, Object object){
      if(object instanceof Float || object instanceof Integer || object instanceof Boolean || object instanceof Long
      || object instanceof String || object instanceof byte[]){
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
      return values.keySet();
  }

  public synchronized int keySize(){
      return values.size();
  }

  /** Appends to the settings log. Used for diagnosis of the save wipe bug. Never throws an error. */
  protected void writeLog(String text){
    if (!logging) return; 
    try{
        logFile.writeString("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "] {" + file.path() + "} " + text + "\n", true);
    }catch(Throwable t){
        Log.err("Failed to write settings logs of file '" + file.path() + "' at '" + logFile.path() + "'", t);
    }
  }
  
  protected static void writeLogStatic(String text){
    try{
        logFile.writeString("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "] " + text + "\n", true);
    }catch(Throwable t){
        Log.err("Failed to write settings logs at '" + logFile.path() + "'", t);
    }
  }
  
}
