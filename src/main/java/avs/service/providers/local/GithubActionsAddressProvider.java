package avs.service.providers.local;

import arc.struct.OrderedSet;
import arc.struct.Seq;
import arc.util.serialization.JsonValue;
import avs.util.network.Subnet;


public class GithubActionsAddressProvider extends avs.service.providers.types.CloudDownloadedAddressProvider {
  public GithubActionsAddressProvider() {
    super("Github Actions", "github-actions", "https://api.github.com/meta");
  }

  @Override
  public Seq<Subnet> extractAddressRanges(JsonValue downloaded) {
    OrderedSet<String> list = new OrderedSet<>();
    list.addAll(downloaded.get("actions").asStringArray());
    return list.toSeq().map(a -> Subnet.createInstance(a)).removeAll(a -> a == null);
  }
}
