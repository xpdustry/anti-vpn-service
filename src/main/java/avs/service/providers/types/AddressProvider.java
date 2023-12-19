package avs.service.providers.types;

import java.net.InetAddress;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Strings;

import avs.service.providers.AddressValidity;
import avs.util.DynamicSettings;
import avs.util.Logger;
import avs.util.PVars;


public abstract class AddressProvider {
  protected Seq<AddressValidity> cache = new Seq<>();
  /* Custom folder from plugin settings folder */
  protected Fi customFolder = null;
  protected String fileExt = "bin";
  protected DynamicSettings cacheFile = null;
  
  public final String name, displayName;
  public final Logger logger;
  
  public boolean enabled = true;
  
  public AddressProvider(String name) { this(name, name.toLowerCase().replace(" ", "-")); }
  public AddressProvider(String displayName, String name) {
    if (name == null || displayName == null || name.isBlank() || displayName.isBlank()) 
      throw new IllegalArgumentException("name or displayName cannot be null or empty");
      
    this.name = name;
    this.displayName = displayName;
    this.logger = new Logger("&ly[" + displayName + "]");
  }
  
  public int cacheSize() {
    return cache.size;
  }

  public boolean load() {
    return loadCache();
  }
  
  public boolean reload() {
    cache.clear();
    getCacheFile().clear();
    return load();
  }
  
  public boolean save() {
    return saveCache();
  }
  
  @SuppressWarnings("unchecked")
  protected boolean loadCache() {
    DynamicSettings file = getCacheFile();
    
    try { 
      file.load();
      cache = file.getJson("cache", Seq.class, Seq::new); 
      logger.debug("Cache loaded");
    } catch (Exception e) { 
      logger.err("Failed to load cache file '@'. ", file.getSettingsFile().path());
      logger.err("Error: @", e.toString()); 
      return false;
    }
    
    return true;
  }
  
  protected boolean saveCache() {
    DynamicSettings file = getCacheFile();
    
    try { 
      file.putJson("cache", cache); 
      logger.debug(file.isModified() ? "Cache saved" : "Cache not modified");
    } catch(Exception e) {
      logger.err("Failed to write cache file '@'.", file.getSettingsFile().path());
      logger.err("Error: @", e.toString());
      return false;
    }
    return true;
  }
  
  /* Check if address is blacklisted.
   * The address format must be validated before this.
   * 
   * Return null if ip is not blacklisted
   */
  public AddressValidity checkIP(String ip) {
    if (!enabled) return null;
    
    try {
      InetAddress inet = InetAddress.getByName(ip); // Normally, never throw an error
      AddressValidity valid = cache.find(v -> v.ip.isInNet(inet));
      // TODO: fire an event
      return valid;

    } catch (Exception e) {
      // TODO: fire an event
      logger.debug("Failed to check ip '@'", ip);
      logger.debug("Error: @", e.toString()); 
    }
    
    return null;
  }
  
  protected DynamicSettings getCacheFile() {
    if (cacheFile == null) {
      cacheFile = new DynamicSettings(getFile());
      cacheFile.setErrorHandler(logger::err);
    }
    return cacheFile;
  }
  
  private Fi folder = null;
  
  protected Fi getFile() {
    if (folder == null) {
      if (customFolder == null) folder = PVars.cacheFolder;  
      else {
        /* Cut the start of path if is the same
         * This avoid to recreate sub-folders named same as the first path
         * 
         * E.g. with this feature you can specifie a custom path starting into 
         * settings folder with PVars. 
         * Without recreates config/mods/...... into the plugin folder.
         */
        
        String[] path1 = PVars.pluginFolder.absolutePath().split("/"), 
                 path2 = customFolder.absolutePath().split("/");
        int best = 0;
        
        while (best < Integer.min(path1.length, path2.length)) {
          if (!path1[best].equals(path2[best])) break;
          best++;
        }
        
        String[] newPath2 = new String[path2.length-best];
        System.arraycopy(path2, best, newPath2, 0, path2.length-best);
  
        folder = PVars.pluginFolder.child(Strings.join("/", newPath2));
      }
    }
    
    return folder.child(name + "." + fileExt);
  }
}
