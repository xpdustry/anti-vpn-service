package avs.service.providers.custom;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import avs.service.IPValidity;


public class UserAddressList extends avs.service.providers.types.CustomAddressProvider {
  private Seq<IPValidity> blacklistedIPs;
  private ObjectMap<IPValidity, avs.util.Subnet> list;
  
  public UserAddressList() {
    super("Custom Server List", "local-custom");
  }

  @Override
  public void load() {
    reloadSettings();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void reloadSettings() {
    if (!Core.settings.has("avs-ip-blacklist")) saveSettings();
    else blacklistedIPs = Core.settings.getJson("avs-ip-blacklist", Seq.class, () -> new Seq<IPValidity>());
  }

  @Override
  public void saveSettings() {
    Core.settings.putJson("avs-ip-blacklist", blacklistedIPs);
  }

  @Override
  public void blockIP(IPValidity address) {
    
    if(blacklistedIPs.addUnique(address)) saveSettings();
  }

  @Override
  public boolean allowIP(IPValidity address) {
    boolean removed = blacklistedIPs.remove(address);
    if (removed) saveSettings();
    return removed;
  }

  @Override
  public IPValidity checkIP(String ip) {
    IPValidity.checkIP(ip);
    return blacklistedIPs.find(v -> v.ip.equals(ip));
  }
}
