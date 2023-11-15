package avs.service.providers.online;

import avs.service.providers.AddressValidity;


public class VpnApiService extends avs.service.providers.types.OnlineAddressProvider {
  public VpnApiService() {
    super("VpnAPI.io Service", "vpnapi", "https://vpnapi.io/api/{0}?key={1}");
    hasTokens = true;
  }

  @Override
  public AddressValidity handleReply(String reply) {
    // TODO Auto-generated method stub
    return null;
  }
}
