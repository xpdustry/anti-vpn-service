package avs.service.providers.types;

import java.net.InetAddress;

import avs.service.providers.AddressValidity;
import avs.util.Logger;


public abstract class AddressProvider {
  protected arc.struct.Seq<AddressValidity> cache;
  public final String name, displayName;
  
  public AddressProvider(String name) { this(name, name.toLowerCase().replace(" ", "-")); }
  public AddressProvider(String displayName, String name) {
    if (name == null || displayName == null || name.isBlank() || displayName.isBlank()) 
      throw new IllegalArgumentException("name or displayName cannot be null or empty");
      
    this.name = name;
    this.displayName = displayName;
  }

  public abstract void load();
  public void reload() {
    cache.clear();
    load();
  }
  public abstract void save();
  
  public AddressValidity checkIP(String ip) {
    try {
      AddressValidity.checkIP(ip);
      InetAddress inet = InetAddress.getByName(ip); // Normally, never throw an error
      AddressValidity valid = cache.find(v -> v.ip.isInNet(inet));
      // TODO: fire an event
      return valid;

    } catch (Exception e) {
      // TODO: fire an event
      Logger.debug("Failed to check ip '@'", ip);
      Logger.debug("Error: @", e.toString()); 
    }
    
    return null;
  }
}
