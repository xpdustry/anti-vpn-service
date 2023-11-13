package avs.service.providers.local;

import arc.struct.Seq;

import avs.util.Subnet;

public class AzureAddressProvider extends avs.service.providers.types.LocalAddressProvider {
  public AzureAddressProvider() {
    super("Azure Cloud", "azure");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
