package avs.service.providers.custom;

import arc.Core;
import arc.struct.Seq;

import avs.service.providers.AddressValidity;


public class WhitelistAddress extends avs.service.providers.types.CustomAddressProvider {
  public WhitelistAddress() {
    super("Whitelist", "whitelist");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void load() {
    if (!Core.settings.has("avs-ip-whitelist")) save();
    else {
      cache = Core.settings.getJson("avs-ip-whitelist", Seq.class, () -> new Seq<AddressValidity>());
      cache.remove(v -> v == null);
    }
  }

  @Override
  public void save() {
    Core.settings.putJson("avs-ip-whitelist", cache);
  }

  @Override
  public void blockAddress(AddressValidity address) {
    if (cache.remove(address)) save();
  }

  @Override
  public boolean allowAddress(AddressValidity address) {
    boolean added = cache.addUnique(address);
    if (added) save();
    return added;
  }
}
