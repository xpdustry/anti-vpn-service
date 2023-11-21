package avs.service.providers.local;

import arc.struct.Seq;

import avs.util.Subnet;

public class AzureAddressProvider extends avs.service.providers.types.CloudDownloadedAddressProvider {
  public AzureAddressProvider() {
    super("Azure Cloud", "azure");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
