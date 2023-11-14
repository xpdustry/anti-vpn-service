package avs.service.providers.types;

import avs.service.providers.IPValidity;

public abstract class OnlineAddressProvider extends AddressProvider {
  public OnlineAddressProvider(String name) { super(name); }
  public OnlineAddressProvider(String displayName, String name) { super(displayName, name); }
  
  
  @Override
  public void load() {
    // TODO Auto-generated method stub
    
  }
  @Override
  public IPValidity checkIP(String ip) {
    // TODO Auto-generated method stub
    return null;
  }

  
}
