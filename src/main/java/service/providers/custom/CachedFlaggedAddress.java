package service.providers.custom;

import arc.struct.Seq;
import service.IPValidity;
import service.providers.types.CustomAddressProvider;


public class CachedFlaggedAddress extends CustomAddressProvider {
  private Seq<IPValidity> blacklistedIPs;
  
  public CachedFlaggedAddress() {
    super("Flagged Address Cache", "flagged-address");
  }

  @Override
  public void load() {
    // TODO Auto-generated method stub
    
  }  
  
  @Override
  public void reloadSettings() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void saveSettings() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void blockIP(String address) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean allowIP(String address) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public IPValidity checkIP(String ip) {
    // TODO Auto-generated method stub
    return null;
  }
}
