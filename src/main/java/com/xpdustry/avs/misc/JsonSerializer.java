/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024-2025 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xpdustry.avs.misc;

import com.xpdustry.avs.command.AVSCommandManager;
import com.xpdustry.avs.command.Command;
import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.misc.address.*;
import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.providers.ProviderAction;
import com.xpdustry.avs.service.providers.type.AddressProvider;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.json.Json2;
import com.xpdustry.avs.util.network.Subnet;

import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import arc.util.serialization.SerializationException;


@SuppressWarnings("rawtypes")
public class JsonSerializer {
  public static void apply(Json2 json) {
    // ¯\_(ツ)_/¯ can be useful
    json.setSerializer(Boolean.class, new Json2.JsonObjectSerializer<>() {
      @Override
      public void write(Json2 json, Boolean object, Class knownType, Class elementType, Class keyType) {
        try { json.getWriter().value(object); } 
        catch (java.io.IOException e) { throw new SerializationException(e); }
      }

      @Override
      public Boolean read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        return jsonData.isNull() ? false : fromKey(json, jsonData.asString(), Boolean.class);
      }

      @Override
      public String toKey(Json2 json, Boolean object, Class knownType) {
        return object.toString();
      }

      @Override
      public Boolean fromKey(Json2 json, String key, Class type) {
        if (Strings.isTrue(key)) return true;
        else if (Strings.isFalse(key)) return false;
        throw new SerializationException("unreconized boolean value");
      }
    });
    
    json.setSerializer(Subnet.class, new Json2.JsonObjectSerializer<>() {
      @Override
      public void write(Json2 json, Subnet object, Class knownType, Class elementType, Class keyType) {
        json.writeValue(toKey(json, object, Subnet.class));
      }

      @Override
      public Subnet read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        return jsonData.isNull() ? null : fromKey(json, jsonData.asString(), Subnet.class);
      }

      @Override
      public String toKey(Json2 json, Subnet object, Class knownType) {
        return object.toString();
      }

      @Override
      public Subnet fromKey(Json2 json, String key, Class type) {
        return Subnet.createInstance(key);
      }
    });
    
    json.setSerializer(AddressInfos.class, new Json2.Serializer<>() {
      @Override
      public void write(Json json, AddressInfos object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("ip", object.ip);
        json.writeValue("location", object.location);
        json.writeValue("ISP", object.ISP);
        json.writeValue("ASN", object.ASN);
        json.writeValue("locale", object.locale.toLanguageTag());
        json.writeValue("longitude", object.longitude);
        json.writeValue("latitude", object.latitude);
        json.writeObjectEnd();
      }

      @Override
      public AddressInfos read(Json json, JsonValue jsonData, Class type) {
        if (jsonData.isNull()) return null;
        AddressInfos infos = new AddressInfos(jsonData.getString("ip"));
        
        infos.location = jsonData.getString("location");
        infos.ISP = jsonData.getString("ISP");
        infos.ASN = jsonData.getString("ASN");
        infos.locale = java.util.Locale.forLanguageTag(jsonData.getString("locale"));
        infos.longitude = jsonData.getFloat("longitude");
        infos.latitude = jsonData.getFloat("latitude");
        
        return infos;
      }
      
    });
    
    json.setSerializer(AddressStats.class, new Json2.Serializer<>() {
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
    
    json.setSerializer(AddressType.class, new Json2.Serializer<>() {
      @Override
      public void write(Json json, AddressType object, Class knownType) {
        json.writeValue(object.toBinary());
      }

      @Override
      public AddressType read(Json json, JsonValue jsonData, Class type) {
        return AddressType.fromBinary(jsonData.asLong());
      }
    });
    
    json.setSerializer(AddressValidity.class, new Json2.Serializer<>() {
      @Override
      public void write(Json json, AddressValidity object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("address", object.subnet);
        if (object.infos != null)
          json.writeValue("infos", object.infos, AddressInfos.class);
        json.writeValue("stats", object.stats);
        json.writeValue("type", object.type);
        json.writeObjectEnd();
      }
      
      @Override
      public AddressValidity read(Json json, JsonValue jsonData, Class type_) {
        Subnet subnet = json.readValue("address", Subnet.class, jsonData);
        if (subnet == null) 
          throw new SerializationException("malformed AddressValidity object; \"address\" is missing");

        AddressInfos infos = json.readValue("infos", AddressInfos.class, jsonData);
        AddressValidity valid = new AddressValidity(subnet, infos);
        AddressStats stats = json.readValue("stats", AddressStats.class, jsonData);
        AddressType type = json.readValue("type", AddressType.class, jsonData);

        if (stats != null) valid.stats = stats;
        if (type != null) valid.type = type;
        return valid;
      }
    });
    
    
    //// Used by RestrictedModeConfig
    json.setSerializer(ProviderAction.class, new Json2.JsonObjectSerializer<>() {
      @Override
      public void write(Json2 json, ProviderAction object, Class knownType, Class elementType, Class keyType) {
        json.writeValue(toKey(json, object, ProviderAction.class));
      }

      @Override
      public ProviderAction read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        return fromKey(json, jsonData.asString(), ProviderAction.class);
      }

      @Override
      public String toKey(Json2 json, ProviderAction object, Class knownType) {
        return object.name;
      }

      @Override
      public ProviderAction fromKey(Json2 json, String key, Class type) {
        ProviderAction a = ProviderAction.get(key);
        if (a == null) throw new SerializationException("no provider action named '"+key+"' found.");
        return a;
      }
    });
    
    json.setInheritSerializer(AddressProvider.class, new Json2.JsonObjectSerializer<>() {
      @Override
      public void write(Json2 json, AddressProvider object, Class knownType, Class elementType, Class keyType) {
        json.writeValue(toKey(json, object, AddressProvider.class));
      }

      @Override
      public AddressProvider read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        return fromKey(json, jsonData.asString(), AddressProvider.class);
      }

      @Override
      public String toKey(Json2 json, AddressProvider object, Class knownType) {
        return object.name;
      }

      @Override
      public AddressProvider fromKey(Json2 json, String key, Class type) {
        AddressProvider provider = AntiVpnService.get(key);
        if (provider == null) throw new SerializationException("no address provider named '"+key+"' found.");
        return provider;
      }
    }); 

    json.setSerializer(AVSConfig.ConfigField.class, new Json2.JsonObjectSerializer<>() {
      @Override
      public void write(Json2 json, AVSConfig.ConfigField object, Class knownType, Class elementType, Class keyType) {
        json.writeValue(toKey(json, object, AVSConfig.ConfigField.class));
      }

      @Override
      public AVSConfig.ConfigField read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        return fromKey(json, jsonData.asString(), AVSConfig.ConfigField.class);
      }

      @Override
      public String toKey(Json2 json, AVSConfig.ConfigField object, Class knownType) {
        return object.name;
      }

      @Override
      public AVSConfig.ConfigField fromKey(Json2 json, String key, Class type) {
        return (AVSConfig.ConfigField) AVSConfig.instance().get(key);
      }
    });
    
    json.setInheritSerializer(Command.class, new Json2.JsonObjectSerializer<>() {
      @Override
      public void write(Json2 json, Command object, Class knownType, Class elementType, Class keyType) {
        json.writeValue(toKey(json, object, Command.class));
      }

      @Override
      public Command read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        return fromKey(json, jsonData.asString(), Command.class);
      }

      @Override
      public String toKey(Json2 json, Command object, Class knownType) {
        return object.name;
      }

      @Override
      public Command fromKey(Json2 json, String key, Class type) {
        return AVSCommandManager.get(key);
      }
    }); 
    
    ///
  }
}
