package avs.service.providers.types;

import java.net.InetAddress;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Strings;

import avs.service.providers.AddressValidity;
import avs.util.Logger;
import avs.util.PVars;


public abstract class AddressProvider {
  protected Seq<AddressValidity> cache = new Seq<>();
  /* Custom folder from plugin settings folder */
  protected Fi customFolder = null;
  
  public final String name, displayName;
  public final Logger logger;
  
  public AddressProvider(String name) { this(name, name.toLowerCase().replace(" ", "-")); }
  public AddressProvider(String displayName, String name) {
    if (name == null || displayName == null || name.isBlank() || displayName.isBlank()) 
      throw new IllegalArgumentException("name or displayName cannot be null or empty");
      
    this.name = name;
    this.displayName = displayName;
    this.logger = new Logger("&ly[" + displayName + "]");
  }

  public boolean load() {
    return loadCache();
  }
  
  public boolean reload() {
    cache.clear();
    return load();
  }
  
  public boolean save() {
    return saveCache();
  }
  
  protected boolean loadCache() {
    Fi cacheFile = getFile();
    Seq<AddressValidity> ips = new Seq<>();
    
    if (cacheFile.exists() && !cacheFile.isDirectory()) {
      try { ips = Seq.with(cacheFile.readString().split("\n"))
                     .map(line -> line.isBlank() || line.strip().startsWith("#") ? null : AddressValidity.fromString(line.strip())); }
      catch (Exception e) { 
        logger.err("Failed to load cache file '@'. ", cacheFile.path());
        logger.err("Error: @", e.toString()); 
        return false;
      }
    } 
    
    ips.removeAll(v -> v == null);
    cache = ips;
    return true;
  }
  
  protected boolean saveCache() {
    Fi cacheFile = getFile();
    
    try {
      cacheFile.writeString("");
      cache.each(s -> cacheFile.writeString(s.toString() + "\n", true));  
      
    } catch(Exception e) {
      logger.err("Failed to write cache file '@'.", cacheFile.path());
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
  
  
  // save the custom folder to avoid doing the operation another time.
  private Fi folder = null;
  
  protected Fi getFile() {
    if (customFolder != null) {
      // Cut already done
      if (folder != null) return folder.child(name + ".txt");
      
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
      return folder.child(name + ".txt");
    }
    return PVars.cacheFolder.child(name + ".txt");
  }
}
