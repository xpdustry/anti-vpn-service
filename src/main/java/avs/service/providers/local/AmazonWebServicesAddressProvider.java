package avs.service.providers.local;

import arc.struct.Seq;
import avs.service.providers.types.CloudDownloadedAddressProvider;
import avs.util.Subnet;

public class AmazonWebServicesAddressProvider extends CloudDownloadedAddressProvider {
  public AmazonWebServicesAddressProvider() {
    super("Amazon Web Services", "amazon-web-services");
  }

  @Override
  public Seq<Subnet> downloadList() {

    // TODO Auto-generated method stub
    return null;
  }

  
}
