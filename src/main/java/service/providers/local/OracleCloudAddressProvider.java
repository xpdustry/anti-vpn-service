package service.providers.local;

import arc.struct.Seq;

import service.providers.types.LocalAddressProvider;
import util.Subnet;

public class OracleCloudAddressProvider extends LocalAddressProvider {
  public OracleCloudAddressProvider() {
    super("Oracle", "oracle");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
