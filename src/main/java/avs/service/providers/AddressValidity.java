package avs.service.providers;

import arc.util.serialization.JsonValue;

import avs.util.Strings;
import avs.util.Subnet;


public class AddressValidity {
  public final Subnet ip;
  public final @arc.util.Nullable AddressInfos infos;
  public boolean isVPN = false, 
      isProxy = false, 
      isTOR = false, 
      isRelay = false;
  
  public AddressValidity(String ip) { this(ip, null); }
  public AddressValidity(String ip, AddressInfos infos) { this(Subnet.createInstance(ip), infos); }
  public AddressValidity(Subnet ip) { this(ip, null); }
  public AddressValidity(Subnet ip, AddressInfos infos) {
    this.ip = ip;
    this.infos = infos;
  }
  
  public boolean isValid() {
    return !isVPN && !isProxy && !isTOR && !isRelay;
  }
  
  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof AddressValidity)) return false;
    final AddressValidity other = (AddressValidity) obj;
    // ignore .infos because is an optional class
    return ip.equals(other.ip) &&
           isVPN == other.isVPN &&
           isProxy == other.isProxy &&
           isTOR == other.isTOR &&
           isRelay == other.isRelay;
  }
  
  @Override
  public final String toString() {
    return toJson().toString();
  }
  
  public JsonValue toJson() {
    JsonValue o = new JsonValue(JsonValue.ValueType.array);
    
    o.addChild(new JsonValue(ip.toString()));
    o.addChild(new JsonValue(Strings.binary2integer(isVPN, isProxy, isTOR, isRelay)));
    if (infos != null) o.addChild(infos.toJson());
    
    return o;
  }
  
  public static AddressValidity fromString(String str) { return fromJson(new arc.util.serialization.JsonReader().parse(str)); }
  public static AddressValidity fromJson(JsonValue content) {
    Subnet net = Subnet.createInstance(content.getString(0));
    boolean[] type = Strings.integer2binary(content.getInt(1), 4);
    AddressInfos infos = AddressInfos.fromJson(content.get(2));
    
    AddressValidity valid = new AddressValidity(net, infos);
    if (type[0]) valid.isVPN = true;
    if (type[1]) valid.isProxy = true;
    if (type[2]) valid.isTOR = true;
    if (type[3]) valid.isRelay = true;   
    
    return valid;    
  }
  
  public static void checkIP(String ip) {
    // TODO: Improve check. E.g. check if is an network, broadcast or loopback address.
    try {
      if (ip.contains("/")) throw new IllegalArgumentException("IP address must not have a mask, only subnet addresses can. (ip: " + ip + ")");
      // Convert the ip to InetAddress, to check its validity.
      // Because we cannot access the class InetAddressUtil class,
      // used to check formatting of address
      java.net.InetAddress.getByName(ip);
    } catch (java.net.UnknownHostException e) { throw new IllegalArgumentException(e.getLocalizedMessage() + ". (ip: " + ip + ")"); }
  }
}
