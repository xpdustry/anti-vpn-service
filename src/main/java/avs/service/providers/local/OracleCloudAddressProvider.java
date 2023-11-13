package avs.service.providers.local;

import arc.struct.Seq;

import avs.util.Subnet;

public class OracleCloudAddressProvider extends avs.service.providers.types.LocalAddressProvider {
  public OracleCloudAddressProvider() {
    super("Oracle", "oracle");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
