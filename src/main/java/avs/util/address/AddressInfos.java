package avs.util.address;


public class AddressInfos {
  public final String ip;
  // Default values if the supplier does not provide all the information
  public String  network = "0.0.0.0/0", 
      location = "<unknown>",
      ISP = "",
      ASN = "ASN00000",
      locale = "en";
  public float longitude = 0, latitude = 0;
  
  public AddressInfos(String ip) {
    this.ip = ip;
  }
}
