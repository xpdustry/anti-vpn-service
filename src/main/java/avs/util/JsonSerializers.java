package avs.util;

import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;

import avs.util.address.*;
import avs.util.network.Subnet;


public class JsonSerializers {
  public static void apply(Json json) {
    json.setSerializer(Subnet.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, Subnet object, Class knownType) {
        json.writeValue(object.toString());
      }

      @Override
      public Subnet read(Json json, JsonValue jsonData, Class type) {
        return jsonData.isNull() ? null : Subnet.createInstance(jsonData.asString());
      }
    });
    
    json.setSerializer(AddressInfos.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, AddressInfos object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("ip", object.ip);
        json.writeValue("network", object.network);
        json.writeValue("location", object.location);
        json.writeValue("ISP", object.ISP);
        json.writeValue("ASN", object.ASN);
        json.writeValue("locale", object.locale);
        json.writeValue("longitude", object.longitude);
        json.writeValue("latitude", object.latitude);
        json.writeObjectEnd();
      }

      @Override
      public AddressInfos read(Json json, JsonValue jsonData, Class type) {
        if (jsonData.isNull()) return null;
        AddressInfos infos = new AddressInfos(jsonData.getString("ip"));
        
        infos.network = jsonData.getString("network");
        infos.location = jsonData.getString("location");
        infos.ISP = jsonData.getString("ISP");
        infos.ASN = jsonData.getString("ASN");
        infos.locale = jsonData.getString("locale");
        infos.longitude = jsonData.getFloat("longitude");
        infos.latitude = jsonData.getFloat("latitude");
        
        return infos;
      }
      
    });
    
    json.setSerializer(AddressStats.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, AddressStats object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("kickNumber", object.kickNumber);
        json.writeObjectEnd();
      }

      @Override
      public AddressStats read(Json json, JsonValue jsonData, Class type) {
        AddressStats stats = new AddressStats();
        
        stats.kickNumber = jsonData.getInt("kickNumber");
        
        return stats;
      }
    });
    
    json.setSerializer(AddressType.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, AddressType object, Class knownType) {
        json.writeValue(Strings.binary2integer(object.vpn, object.proxy, object.tor, object.relay, object.other));
      }

      @Override
      public AddressType read(Json json, JsonValue jsonData, Class type) {
        boolean[] types = Strings.integer2binary(jsonData.asLong(), AddressType.numberOfTypes);
        AddressType aType = new AddressType();
        
        aType.vpn = types[0];
        aType.proxy = types[1];
        aType.tor = types[2];
        aType.relay = types[3];
        aType.other = types[4];
        
        return aType;
      }
    });
    
    json.setSerializer(AddressValidity.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, AddressValidity object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("ip", object.ip);
        json.writeValue("infos", object.infos, AddressInfos.class);
        json.writeValue("stats", object.stats);
        json.writeValue("type", object.type);
        json.writeObjectEnd();
      }
      @Override
      public AddressValidity read(Json json, JsonValue jsonData, Class type) {
        Subnet subnet = json.getSerializer(Subnet.class).read(json, jsonData.get("ip"), Subnet.class);
        if (subnet == null) return null;
        AddressInfos infos = json.getSerializer(AddressInfos.class).read(json, jsonData.get("infos"), AddressInfos.class);
        AddressStats stats = json.getSerializer(AddressStats.class).read(json, jsonData.get("stats"), AddressStats.class);
        AddressType type_ = json.getSerializer(AddressType.class).read(json, jsonData.get("type"), AddressType.class);
        
        AddressValidity valid = new AddressValidity(subnet, infos);
        if (stats != null) valid.stats = stats;
        if (type_ != null) valid.type = type_;
        return valid;
      }
    });
  }
}
