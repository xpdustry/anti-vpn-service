package avs.service.providers.types;

import arc.files.Fi;
import arc.struct.Seq;

import avs.service.providers.AddressValidity;
import avs.util.Logger;
import avs.util.PVars;


public abstract class OnlineAddressProvider extends AddressProvider {
  /* 
   * The URL of service to use. 
   * Don't forgot to add '{0}' = the ip, '{1}' = the (optional), in the URL.
   */
  protected final String url;
  
  protected Seq<String> tokens = new Seq<>();
  protected boolean hasTokens = false;
  
  public OnlineAddressProvider(String name, String url) { 
    super(name);
    this.url = url;
    if (url == null || url.isBlank()) throw new NullPointerException("url is empty");
    customFolder = PVars.tokensFolder;
  }
  public OnlineAddressProvider(String displayName, String name, String url) { 
    super(displayName, name); 
    this.url = url;
    if (url == null || url.isBlank()) throw new NullPointerException("url is empty");
    customFolder = PVars.tokensFolder;
  }
  
  @Override
  public void reload() {
    tokens.clear();
    load();
  }
  
  //Cache are tokens for online services
  @Override
  protected void loadCache() {
    // No tokens needed. skip this
    if (!hasTokens) {
      Logger.info("Loaded service '@'", displayName);
      return;
    }
    
    Fi tokensFile = getFile();
    Seq<String> tokens_ = new Seq<>();
    
    if (tokensFile.exists() && !tokensFile.isDirectory()) {
      try { tokens_ = Seq.with(tokensFile.readString().split("\n")).map(line -> line.strip()); }
      catch (Exception e) { 
        Logger.err("Failed to load tokens file '@'.", tokensFile.path(), displayName);
        Logger.err("Error: " + e.toString());        
      }
    } 
    
    tokens_.removeAll(v -> v.isBlank() || v.startsWith("#"));
    tokens = tokens_;
    
    if (tokens.isEmpty()) Logger.err("Failed to load tokens from service '@'! Skipping it...", displayName);
    else Logger.info("Loaded @ tokens for service '@'.", tokens.size, displayName);
  }
  
  // Cache are tokens for online services
  @Override
  protected void saveCache() {
    // No tokens needed. skip this
    if (!hasTokens) return;
    
    Fi tokensFile = getFile();
    
    try {
      tokensFile.writeString("");
      tokens.each(t -> tokensFile.writeString(t + "\n", true));
      
    } catch (Exception e) {
      Logger.err("Failed to write tokens file '@'.", tokensFile.path());
      Logger.err("Error: @", e.toString());
    }
  }
  
  public boolean addToken(String token) {
    // TODO Auto-generated method stub
    return false;
  }
  
  public boolean removeToken(String token) {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public AddressValidity checkIP(String ip) {
    // TODO Auto-generated method stub
    return null;
  }

  public abstract AddressValidity handleReply(String reply);
}
