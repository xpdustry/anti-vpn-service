package avs.service.providers.custom;

import arc.Core;
import arc.struct.Seq;

import avs.service.providers.AddressValidity;


public class UserAddressList extends avs.service.providers.types.CustomAddressProvider {
  public UserAddressList() {
    super("Custom Blacklist", "custom-blacklist");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void load() {
    if (!Core.settings.has("avs-ip-blacklist")) save();
    else {
      cache = Core.settings.getJson("avs-ip-blacklist", Seq.class, () -> new Seq<AddressValidity>());
      cache.remove(v -> v == null);
    }
  }

  @Override
  public void save() {
    Core.settings.putJson("avs-ip-blacklist", cache);
  }
}
