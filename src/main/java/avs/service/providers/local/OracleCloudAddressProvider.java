package avs.service.providers.local;

import arc.struct.OrderedSet;
import arc.struct.Seq;
import arc.util.serialization.JsonValue;
import avs.util.network.Subnet;


public class OracleCloudAddressProvider extends avs.service.providers.types.CloudDownloadedAddressProvider {
  public OracleCloudAddressProvider() {
    super("Oracle", "oracle", "https://docs.cloud.oracle.com/en-us/iaas/tools/public_ip_ranges.json");
  }

  @Override
  public Seq<Subnet> extractAddressRanges(JsonValue downloaded) {
    OrderedSet<String> list = new OrderedSet<>();
    
    for (JsonValue values=downloaded.get("regions").child; values!=null; values=values.next) {
      for (JsonValue entry=values.get("cidrs").child; entry!=null; entry=entry.next) {
        list.add(entry.getString("cidr"));
      }
    }
    
    return list.toSeq().map(a -> Subnet.createInstance(a)).removeAll(a -> a == null);
  }
}
