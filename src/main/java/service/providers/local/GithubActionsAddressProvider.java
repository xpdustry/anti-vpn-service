package service.providers.local;

import arc.struct.Seq;

import service.providers.types.LocalAddressProvider;
import util.Subnet;

public class GithubActionsAddressProvider extends LocalAddressProvider {
  public GithubActionsAddressProvider() {
    super("Github Actions", "github-actions");
  }

  @Override
  public Seq<Subnet> downloadList() {
    // TODO Auto-generated method stub
    return null;
  }

}
