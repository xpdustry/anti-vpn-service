package avs.service;

public class IPInfos {
  public final String ip;
  // Default values if the supplier does not provide all the information
  public String  network = "0.0.0.0/0", 
      location = "<unknown>",
      ISP = "", 
      locale = "en";
  public float longitute = 0, latitude = 0;
  
  public IPInfos(String ip) {
    this.ip = ip;
  }
}
