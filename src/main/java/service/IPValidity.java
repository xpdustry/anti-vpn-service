package service;

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
}
