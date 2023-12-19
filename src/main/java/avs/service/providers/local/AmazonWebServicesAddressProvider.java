 package avs.service.providers.local;

import arc.struct.OrderedSet;
import arc.struct.Seq;
import arc.util.serialization.JsonValue;

import avs.util.Subnet;


public class AmazonWebServicesAddressProvider extends avs.service.providers.types.CloudDownloadedAddressProvider {
  public AmazonWebServicesAddressProvider() {
    super("Amazon Web Services", "amazon-web-services", "https://ip-ranges.amazonaws.com/ip-ranges.json");
  }

  @Override
  public Seq<Subnet> extractAddressRanges(JsonValue downloaded) {
    OrderedSet<String> list = new OrderedSet<>();
    
    for (JsonValue values=downloaded.get("prefixes").child; values!=null; values=values.next)
      list.add(values.getString("ip_prefix"));
    for (JsonValue values=downloaded.get("ipv6_prefixes").child; values!=null; values=values.next)
      list.add(values.getString("ipv6_prefix"));
    
    return list.toSeq().map(a -> Subnet.createInstance(a)).removeAll(a -> a == null);
  }
}
