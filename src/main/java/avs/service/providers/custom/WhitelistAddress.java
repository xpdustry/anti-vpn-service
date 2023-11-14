package avs.service.providers.custom;

import arc.Core;
import arc.struct.Seq;
import avs.service.providers.IPValidity;
import avs.service.providers.types.CustomAddressProvider;

public class WhitelistAddress extends CustomAddressProvider {
  private Seq<IPValidity> whitelist;
  
  public WhitelistAddress() {
    super("Whitelist", "whitelist");
  }

  @Override
  public void load() {
    reloadSettings();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void reloadSettings() {
    if (!Core.settings.has("avs-ip-whitelist")) saveSettings();
    else whitelist = Core.settings.getJson("avs-ip-whitelist", Seq.class, () -> new Seq<IPValidity>());
  }

  @Override
  public void saveSettings() {
    Core.settings.putJson("avs-ip-whitelist", whitelist);
  }

  @Override
  public void blockAddress(IPValidity address) {
    if (whitelist.remove(address)) saveSettings();  
  }

  @Override
  public boolean allowAddress(IPValidity address) {
    boolean added = whitelist.addUnique(address);
    if(added) saveSettings();
    return added;
  }

  @Override
  public IPValidity checkIP(String ip) {
    // TODO: Check with Subnet.isInNet() instead
    
    IPValidity.checkIP(ip);
    return whitelist.find(v -> v.ip.equals(ip));
  }
}
