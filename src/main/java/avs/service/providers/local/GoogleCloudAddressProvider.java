package avs.service.providers.local;

import arc.struct.Seq;

import avs.util.Subnet;

public class GoogleCloudAddressProvider extends avs.service.providers.types.LocalAddressProvider {

  public GoogleCloudAddressProvider() {
    super("Google", "google");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
