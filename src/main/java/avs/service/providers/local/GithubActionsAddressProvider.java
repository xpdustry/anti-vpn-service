package avs.service.providers.local;

import arc.struct.Seq;

import avs.util.Subnet;

public class GithubActionsAddressProvider extends avs.service.providers.types.CloudDownloadedAddressProvider {
  public GithubActionsAddressProvider() {
    super("Github Actions", "github-actions");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
