package service.providers.custom;

import arc.Core;
import arc.struct.Seq;
import service.IPValidity;
import service.providers.types.CustomAddressProvider;


public class UserAddressList extends CustomAddressProvider {

  public UserAddressList() {
    super("Custom Server List", "local-custom");
  }

  @Override
  public void reloadSettings() {
    Core.settings.defaults("avs-ip-blacklist", new Seq<>());
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
  public void load() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public IPValidity checkIP(String ip) {
    // TODO Auto-generated method stub
    return null;
  }

}
