package service.providers.local;

import arc.struct.Seq;

import service.providers.types.LocalAddressProvider;
import util.Subnet;

public class AzureAddressProvider extends LocalAddressProvider {
  public AzureAddressProvider() {
    super("Azure Cloud", "azure");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
