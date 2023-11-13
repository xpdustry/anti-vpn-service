package avs.service.providers.types;

public abstract class AddressProvider {
  public final String name, displayName;
  
  public AddressProvider(String name) { this(name, name.toLowerCase().replace(" ", "-")); }
  public AddressProvider(String displayName, String name) {
    if (name == null || displayName == null || name.isBlank() || displayName.isBlank()) 
      throw new IllegalArgumentException("name or displayName cannot be null or empty");
      
    this.name = name;
    this.displayName = displayName;
  }

  public abstract void load();
  public abstract avs.service.IPValidity checkIP(String ip);
}
