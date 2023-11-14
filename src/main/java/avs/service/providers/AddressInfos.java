package avs.service.providers;

import arc.util.serialization.JsonValue;

public class AddressInfos {
  public final String ip;
  // Default values if the supplier does not provide all the information
  public String  network = "0.0.0.0/0", 
      location = "<unknown>",
      ISP = "", 
      locale = "en";
  public float longitude = 0, latitude = 0;
  
  public AddressInfos(String ip) {
    this.ip = ip;
  }
  
  @Override
  public final String toString() {
    return toJson().toString();
  }
  
  public JsonValue toJson() {
     JsonValue o = new JsonValue(JsonValue.ValueType.array);
    
    o.addChild(new JsonValue(ip));
    o.addChild(new JsonValue(network));
    o.addChild(new JsonValue(location));
    o.addChild(new JsonValue(ISP));
    o.addChild(new JsonValue(locale));
    o.addChild(new JsonValue(longitude));
    o.addChild(new JsonValue(latitude));
    
    return o;   
  }
  
  public static AddressInfos fromString(String str) { return fromJson(new arc.util.serialization.JsonReader().parse(str)); }
  public static AddressInfos fromJson(JsonValue content) {
    if (content == null) return null;
    AddressInfos n = new AddressInfos(content.getString(0));
    
    n.network = content.getString(1);
    n.location = content.getString(2);
    n.ISP = content.getString(3);
    n.locale = content.getString(4);
    n.longitude = content.getFloat(5);
    n.latitude = content.getFloat(6);
    
    return n;
  }
}
