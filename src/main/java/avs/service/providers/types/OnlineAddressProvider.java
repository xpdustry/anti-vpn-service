package avs.service.providers.types;

import avs.service.providers.AddressValidity;

public abstract class OnlineAddressProvider extends AddressProvider {
  private String url, token;
  
  public OnlineAddressProvider(String name) { super(name); }
  public OnlineAddressProvider(String displayName, String name) { super(displayName, name); }
  
  
  @Override
  public void load() {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void save() {
    // TODO Auto-generated method stub
    
  }  
  
  @Override
  public AddressValidity checkIP(String ip) {
    // TODO Auto-generated method stub
    return null;
  }
}
