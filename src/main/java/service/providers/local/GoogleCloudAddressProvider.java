package service.providers.local;

import arc.struct.Seq;

import service.providers.types.LocalAddressProvider;
import util.Subnet;

public class GoogleCloudAddressProvider extends LocalAddressProvider {

  public GoogleCloudAddressProvider() {
    super("Google", "google");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
