package avs.service.providers.online;

import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;


public class VpnApiService extends avs.service.providers.types.OnlineServiceAddressProvider {
  public VpnApiService() {
    super("VpnAPI.io Service", "vpnapi", "https://vpnapi.io/api/{0}?key={1}");
    hasTokens = true;
  }

  @Override
  public ServiceReply handleReply(String content) throws Exception {
    JsonValue soup = new JsonReader().parse(content);
    ServiceReply reply = new ServiceReply();
    
    if (soup.size == 0) reply.type = ServiceReplyType.ERROR;
    else if (soup.has("message")) {
      reply.message = soup.getString("message");
      String message = reply.message.toLowerCase();
      
      if (message.contains("invalid api key")) 
        reply.type = ServiceReplyType.INVALID_TOKEN;
      else if (message.contains("is not a valid ip") ||
               message.contains("is a unspecified ip") ||
               message.contains("is a loopback ip") ||
               message.contains("is a private ip")) 
        reply.type = ServiceReplyType.ERROR;
      else if (message.contains("exceeded the maximum daily limit")) 
        reply.type = ServiceReplyType.LIMIT_REACHED;
    }
      
    
    
    // TODO Auto-generated method stub
    return reply;
  }
}
