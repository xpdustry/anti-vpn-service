package avs.service.providers.local;

import arc.struct.OrderedSet;
import arc.struct.Seq;
import arc.util.serialization.JsonValue;

import avs.util.Subnet;


public class GoogleCloudAddressProvider extends avs.service.providers.types.CloudDownloadedAddressProvider {
  public GoogleCloudAddressProvider() {
    super("Google", "google", "https://www.gstatic.com/ipranges/cloud.json");
  }

  @Override
  public Seq<Subnet> extractAddressRanges(JsonValue downloaded) {
    OrderedSet<String> list = new OrderedSet<>();
    int i = 0;
    
    for (JsonValue values=downloaded.get("prefixes").child; values!=null; values=values.next, i++) {
      JsonValue prefixe = values.get("ipv4Prefix");
      if (prefixe != null) list.add(prefixe.asString());
      else {
        prefixe = values.get("ipv6Prefix");
        if (prefixe != null) list.add(prefixe.asString());
        else logger.debug("Could not found address prefixe at index @ from fetched json", i);
      }
    }
    
    return list.toSeq().map(a -> Subnet.createInstance(a)).removeAll(a -> a == null);
  }
}
