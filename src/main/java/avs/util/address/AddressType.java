package avs.util.address;

public class AddressType {
  // Number of address types, this facilitate the coding of boolean values
  public static final int numberOfTypes = 5;
  
  public boolean vpn = false, 
      proxy = false, 
      tor = false, 
      relay = false,
      other = false;
  
  public boolean isNotValid() {
    return vpn || proxy || tor || relay || other;
  }
  
  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof AddressType)) return false;
    final AddressType type = (AddressType) obj;
    return vpn == type.vpn &&
           proxy == type.proxy &&
           tor == type.tor &&
           relay == type.relay &&
           other == type.other;
  }
}
