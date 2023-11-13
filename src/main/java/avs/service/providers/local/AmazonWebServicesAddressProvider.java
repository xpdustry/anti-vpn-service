package avs.service.providers.local;

import arc.struct.Seq;
import avs.service.providers.types.LocalAddressProvider;
import avs.util.Subnet;

public class AmazonWebServicesAddressProvider extends LocalAddressProvider {
  public AmazonWebServicesAddressProvider(String displayName, String name) {
    super("Amazon Web Services", "amazon-web-services");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
