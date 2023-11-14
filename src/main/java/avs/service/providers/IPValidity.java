package avs.service.providers;

public class IPValidity {
  public final String ip;
  public final @arc.util.Nullable IPInfos infos;
  public boolean isVPN = false, 
      isProxy = false, 
      isTOR = false, 
      isRelay = false;
  
  public IPValidity(String ip) { this(ip, null); }
  public IPValidity(String ip, IPInfos infos) {
    this.ip = ip;
    this.infos = infos;
  }
  
  public boolean isValid() {
    return !isVPN && !isProxy && !isTOR && !isRelay;
  }
  
  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof IPValidity)) return false;
    final IPValidity other = (IPValidity) obj;
    // ignore .infos because is an optional class
    return ip == other.ip &&
           isVPN == other.isVPN &&
           isProxy == other.isProxy &&
           isTOR == other.isTOR &&
           isRelay == other.isRelay;
  }
  
  
  public static void checkIP(String ip) {
    try {
      if (ip.contains("/")) throw new IllegalArgumentException("IP address must not have a mask, only subnet addresses can. (ip: " + ip + ")");
      // Convert the ip to InetAddress, to check its validity.
      // Because we cannot access the class InetAddressUtil class,
      // used to check formatting of address
      java.net.InetAddress.getByName(ip);
    } catch (java.net.UnknownHostException e) { throw new IllegalArgumentException(e.getLocalizedMessage() + ". (ip: " + ip + ")"); }
  }
}
