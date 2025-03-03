/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2025 Xpdustry
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

package com.xpdustry.avs.util.json;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import arc.files.Fi;
import arc.struct.*;
import arc.util.Reflect;

import arc.util.serialization.*;
import arc.util.serialization.Json.Serializer;


/**
 * A class that fixes common problems with the actual {@link Json} serializer. <br>
 * It's using a wrong implementation of map serialization.
 * And doesn't handle inherited types serialization.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Json2 extends Json {
  private String typeName = "class";
  private final ArrayMap<Class, Serializer> inheritSerializers = new ArrayMap();

  public Json2() {
  }

  public Json2(JsonWriter.OutputType outputType) {
    super(outputType);
  }

  @Override
  public void setTypeName(String typeName){
    this.typeName = typeName;
  }
  
  public boolean isTypeNameSet() {
    return typeName != null;
  }
  
  /** Try to find a serializer using {@link #getSerializer(Class)} and {@link #getInheritSerializer(Class)} */
  public <T> Serializer<T> findSerializer(Class<T> type) {
    Serializer<T> serializer = getSerializer(type);
    return serializer != null ? serializer : getInheritSerializer(type);
  }
  
  /* Registers a serializer for the parent's class that will be inherited. */
  public <T> void setInheritSerializer(Class<T> type, Serializer<T> serializer) {
    inheritSerializers.put(type, serializer); //TODO: set hierarchy
  }
  
  ///** Set a serializer after the {@code beforeType} type */
  //public <T> void setInheritSerializerBefore(Class<T> type, Class beforeType, Serializer<T> serializer) {
  //
  //}

  public <T> Serializer<T> getInheritSerializer(Class<T> type) {
    for (int i=inheritSerializers.size-1; i>=0; i--) {
      if (inheritSerializers.getKeyAt(i).isAssignableFrom(type)) 
        return inheritSerializers.getValueAt(i);
    }
    return null;
  }
 
  @Override
  public String toJson(Object object, Class knownType, Class elementType) {
    return toJson(object, knownType, elementType, (Class)null);
  }
  
  public String toJson(Object object, Class knownType, Class elementType, Class keyType) {
    StringWriter buffer = new StringWriter();
    toJson(object, knownType, elementType, keyType, buffer);
    return buffer.toString();
  }
  
  @Override
  public void toJson(Object object, Class knownType, Class elementType, Fi file) {
    toJson(object, knownType, elementType, null, file);
  }
  
  public void toJson(Object object, Class knownType, Class elementType, Class keyType, Fi file) {
    try (Writer writer = file.writer(false)) { toJson(object, knownType, elementType, keyType, writer); } 
    catch (Exception ex) { throw new SerializationException("Error writing file: " + file, ex); }
  }
  
  @Override
  public void toJson(Object object, Class knownType, Class elementType, Writer writer) {
    toJson(object, knownType, elementType, null, writer);
  }
  
  public void toJson(Object object, Class knownType, Class elementType, Class keyType, Writer writer) {
    setWriter(new JsonWriter(writer));
    try { writeValue(object, knownType, elementType, keyType); } 
    finally { arc.util.io.Streams.close(getWriter()); }
  }
  
  @Override
  public void writeValue(Object value, Class knownType, Class elementType) {
    writeValue(value, knownType, elementType, null);
  }
  
  public void writeValue(Object value, Class knownType, Class elementType, Class keyType) {
    if (knownType != null && knownType.isAnonymousClass()) {
      knownType = knownType.getSuperclass();
    }
  
    try{
      if (value == null) {
        getWriter().value(null);
        return;
      }

      if ((knownType != null && knownType.isPrimitive()) || knownType == String.class || Reflect.isWrapper(knownType)) {
        getWriter().value(value);
        return;
      }

      Class actualType = value.getClass().isAnonymousClass() ? value.getClass().getSuperclass() : value.getClass();

      if (actualType.isPrimitive() || actualType == String.class || Reflect.isWrapper(actualType)) {
          writeObjectStart(actualType, null);
          writeValue("value", value);
          writeObjectEnd();
          return;
      }

      if (value instanceof JsonSerializable) {
        writeObjectStart(actualType, knownType);
        ((JsonSerializable)value).write(this);
        writeObjectEnd();
        return;
      }

      Serializer serializer = findSerializer(actualType);
      if (serializer != null) {
        if (serializer instanceof JsonSerializer)
          ((JsonSerializer)serializer).write(this, value, knownType, elementType, keyType);
        else serializer.write(this, value, knownType);
        return;
      }

      // Java arrays (e.g. Object[]) special case
      if(actualType.isArray()){
        if(elementType == null) elementType = actualType.getComponentType();
        writeArrayStart();
        for(int i=0; i<java.lang.reflect.Array.getLength(value); i++)
          writeValue(java.lang.reflect.Array.get(value, i), elementType, null);
        writeArrayEnd();
        return;
      }
      
      writeObjectStart(actualType, knownType);
      writeFields(value);
      writeObjectEnd();
      
    } catch (java.io.IOException e) { 
      throw new SerializationException(e); 
    }
  }

  public <T> T fromJson(Class<T> type, Class elementType, Class keytype, java.io.Reader reader) {
    return readValue(type, elementType, new JsonReader().parse(reader), keytype);
  }
  
  public <T> T fromJson(Class<T> type, Class elementType, Class keytype, java.io.InputStream input) {
    return readValue(type, elementType, new JsonReader().parse(input), keytype);
  }
  
  public <T> T fromJson(Class<T> type, Class elementType, Class keytype, Fi file ){
    try { return readValue(type, elementType, new JsonReader().parse(file), keytype); } 
    catch (Exception ex) { throw new SerializationException("Error reading file: " + file, ex); }
  }
  
  public <T> T fromJson(Class<T> type, Class elementType, Class keytype, char[] data, int offset, int length) {
    return readValue(type, elementType, new JsonReader().parse(data, offset, length), keytype);
  }
  
  public <T> T fromJson(Class<T> type, Class elementType, Class keytype, String json) {
    return readValue(type, elementType, new JsonReader().parse(json), keytype);
  }

  @Override
  public <T> T readValue(Class<T> type, Class elementType, JsonValue jsonData, Class keytype) {
    if (jsonData == null) return null;
    if (jsonData.isArray() && (type == null || type == Object.class)) type = (Class<T>)Seq.class;
    if (type == null) return super.readValue(type, elementType, jsonData, keytype);

    if (jsonData.isObject()) {
      if (isTypeNameSet()) {
        String className = jsonData.getString(typeName, null);
        if (className != null) {
          jsonData.remove(typeName);
          type = getClass(className);
          if (type == null) {
            try { type = (Class<T>)Class.forName(className); } 
            catch (Throwable e) { throw new SerializationException(e); }
          }
        }
      }
      
      if (type.isPrimitive() || type == String.class || Reflect.isWrapper(type))
        return readValue("value", type, jsonData);
    }
    
    if (JsonSerializable.class.isAssignableFrom(type)) {
      // A Serializable may be read as an array, string, etc.
      Object object = newInstance(type);
      ((JsonSerializable)object).read(this, jsonData);
      return (T)object;
    }
    
    Serializer serializer = findSerializer(type);
    if (serializer != null) {
      if (serializer instanceof JsonSerializer)
        return (T)((JsonSerializer)serializer).read(this, jsonData, type, elementType, keytype);
      else return (T)serializer.read(this, jsonData, type);
    }
    
    if (jsonData.isObject()) {
      Object object = newInstance(type);
      readFields(object, jsonData);
      return (T)object;
    }
    
    // Java arrays (e.g. Object[]) special case
    if (type.isArray()) {
      Class componentType = type.getComponentType();
      if (elementType == null) elementType = componentType;
      Object result = java.lang.reflect.Array.newInstance(componentType, jsonData.size);
      int i = 0;
      for (JsonValue child=jsonData.child; child!=null; child=child.next)
        java.lang.reflect.Array.set(result, i++, readValue(elementType, null, child));
      return (T)result;
    }
    
    return super.readValue(type, elementType, jsonData, keytype);
  }
 
  protected String convertToString(Object value, Class knownType) {
    if (value == null) return String.valueOf(value);

    Class actualType = value.getClass();
    if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
    
    Serializer serializer = findSerializer(actualType);
    if (serializer != null && serializer instanceof JsonObjectSerializer) {
      if (knownType != null && knownType.isAnonymousClass()) knownType = knownType.getSuperclass();
      return ((JsonObjectSerializer)serializer).toKey(this, value, knownType);
    }
      
    return super.convertToString(value);
  }
  
  protected <T> T convertFromString(String value, Class type) {
    if (value == null) return null;

    if (type != null) {
      Serializer serializer = findSerializer(type);
      if (serializer != null && serializer instanceof JsonObjectSerializer) {
        return (T)((JsonObjectSerializer)serializer).fromKey(this, value, type);
      }  
    }
    
    return (T)value;
  }

  
  /** 
   * Patched {@link Serializer} to handle {@code elementType}, {@code keyType}. <br>
   * <strong>WARNING:</strong> Must only be used with {@link Json2}.
   */
  public static interface JsonSerializer<T> extends Serializer<T> {
    default void write(Json json, T object, Class knownType) {
      write((Json2) json, object, knownType, null, null);
    }
    default T read(Json json, JsonValue jsonData, Class type) {
      return read((Json2) json, jsonData, type, null, null);
    }
   
    void write(Json2 json, T object, Class knownType, Class elementType, Class keyType) ;
    T read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType);
  }
  
  /** Handle key serialization for map types. */
  public static interface JsonObjectSerializer<T> extends JsonSerializer<T> {
    String toKey(Json2 json, T object, Class knownType);
    T fromKey(Json2 json, String key, Class type);
  }

  
  /** Adds default serializers */
  {
    // Enum
    setInheritSerializer(Enum.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, Enum object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        try {
          if (json.isTypeNameSet() && (knownType == null || knownType != actualType)){
            // Ensures that enums with specific implementations (abstract logic) serialize correctly.
            if(actualType.getEnumConstants() == null) actualType = actualType.getSuperclass();
  
            json.writeObjectStart(actualType, null);
            json.getWriter().set("value", json.convertToString(object));
            json.writeObjectEnd();
          }else{
            json.getWriter().value(json.convertToString(object));
          }          
        } catch (java.io.IOException e) { 
          throw new SerializationException(e); 
        }
      }

      @Override
      public Enum read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        if (jsonData.isObject()) jsonData = jsonData.get("value");
        String string = jsonData.asString();
        Enum[] constants = (Enum[])type.getEnumConstants();
        for(int i = 0, n = constants.length; i < n; i++){
            Enum e = constants[i];
            if(string.equals(json.convertToString(e))) return e;
        }
        throw new SerializationException("unknown enum field '" + string + "' for type: " + type.getName());
      }
    });
    
    // Array serializers
    setInheritSerializer(Seq.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, Seq object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if (knownType != null && actualType != knownType && actualType != Seq.class)
          throw new SerializationException("Serialization of an Array other than the known type is not supported.\n"
          + "Known type: " + knownType + "\nActual type: " + actualType);
        json.writeArrayStart();
        Seq array = (Seq)object;
        for (int i=0, n=array.size; i<n; i++)
          json.writeValue(array.get(i), elementType, null);
        json.writeArrayEnd();
      }

      @Override
      public Seq read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        Seq result = type == Seq.class ? new Seq() : (Seq)json.newInstance(type);
        for (JsonValue child=jsonData.child; child!=null; child=child.next)
          result.add(json.readValue(elementType, null, child));
        return result;
      }
    });
    
    setInheritSerializer(ObjectSet.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, ObjectSet object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if (knownType == null) knownType = ObjectSet.class;
        json.writeObjectStart(actualType, knownType);
        json.writeArrayStart("values");
        for (Object entry : (ObjectSet)object)
          json.writeValue(entry, elementType, null);
        json.writeArrayEnd();
        json.writeObjectEnd();
      }

      @Override
      public ObjectSet read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        ObjectSet result = type == ObjectSet.class ? new ObjectSet() : (ObjectSet)json.newInstance(type);
        for (JsonValue child=jsonData.child; child!=null; child=child.next)
          result.add(readValue(elementType, null, child));
        return result;
      }
    });
    
    setInheritSerializer(IntSet.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, IntSet object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if (knownType == null) knownType = IntSet.class;
        json.writeObjectStart(actualType, knownType);
        json.writeArrayStart("values");
        for (IntSet.IntSetIterator iter=((IntSet)object).iterator(); iter.hasNext; )
          json.writeValue(iter.next(), Integer.class, null);
        json.writeArrayEnd();
        json.writeObjectEnd();
      }

      @Override
      public IntSet read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        IntSet result = type == IntSet.class ? new IntSet() : (IntSet)json.newInstance(type);
        for (JsonValue child=jsonData.getChild("values"); child!=null; child=child.next)
          result.add(child.asInt());
        return result;
      }
    });
    
    setInheritSerializer(IntSeq.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, IntSeq object, Class knownType, Class elementType, Class keyType) {
        json.writeArrayStart();
        IntSeq array = (IntSeq)object;
        for (int i=0, n=array.size; i<n; i++)
          json.writeValue(array.get(i), Integer.class, null);
        json.writeArrayEnd();
      }

      @Override
      public IntSeq read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        IntSeq result = type == IntSeq.class ? new IntSeq() : (IntSeq)newInstance(type);
        for (JsonValue child=jsonData.child; child!=null; child=child.next)
          result.add(child.asInt());
        return result;
      }
    });
    
    setInheritSerializer(Queue.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, Queue object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if (knownType != null && actualType != knownType && actualType != Queue.class)
          throw new SerializationException("Serialization of a Queue other than the known type is not supported. "
                                         + "Known type: " + knownType + ". Actual type: " + actualType);
        json.writeArrayStart();
        Queue queue = (Queue)object;
        for (int i=0, n=queue.size; i<n; i++)
          json.writeValue(queue.get(i), elementType, null);
        json.writeArrayEnd();
      }

      @Override
      public Queue read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        Queue result = type == Queue.class ? new Queue() : (Queue)newInstance(type);
        for (JsonValue child=jsonData.child; child!=null; child=child.next)
          result.addLast(readValue(elementType, null, child));
        return result;
      }
    });
    
    setInheritSerializer(Collection.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, Collection object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if (json.isTypeNameSet() && actualType != java.util.ArrayList.class && 
            (knownType == null || knownType != actualType)) {
          json.writeObjectStart(actualType, knownType);
          json.writeArrayStart("items");
          for (Object item : (Collection)object)
            json.writeValue(item, elementType, null);
          json.writeArrayEnd();
          json.writeObjectEnd();
        } else {
          json.writeArrayStart();
          for (Object item : (Collection)object)
            json.writeValue(item, elementType, null);
          json.writeArrayEnd();
        }
      }

      @Override
      public Collection read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        if (jsonData.isObject())  jsonData = jsonData.get("items");

        Collection result = type.isInterface() ? new java.util.ArrayList() : (Collection)newInstance(type);
        for (JsonValue child=jsonData.child; child!=null; child=child.next)
          result.add(readValue(elementType, null, child));
        return result;
      }
    });
    
    // Map serializers
    setInheritSerializer(ObjectMap.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, ObjectMap object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if (knownType == null) knownType = ObjectMap.class;
        json.writeObjectStart(actualType, knownType);
        for (ObjectMap.Entry entry : ((ObjectMap<?, ?>)object).entries())
          json.writeValue(json.convertToString(entry.key, keyType), entry.value, elementType);
        json.writeObjectEnd();
      }

      @Override
      public ObjectMap read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        ObjectMap result = type == ObjectMap.class ? new ObjectMap() : (ObjectMap)newInstance(type);
        for(JsonValue child = jsonData.child; child != null; child = child.next)
          result.put(json.convertFromString(child.name, keyType), json.readValue(elementType, null, child));
        return result;
      }
    });
    
    setInheritSerializer(ObjectIntMap.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, ObjectIntMap object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if (knownType == null) knownType = ObjectIntMap.class;
        json.writeObjectStart(actualType, knownType);
        try{ 
          for (ObjectIntMap.Entry entry : ((ObjectIntMap<?>)object).entries())
            json.getWriter().set(json.convertToString(entry.key, keyType), entry.value);         
        } catch (java.io.IOException e) { 
          throw new SerializationException(e); 
        }
        json.writeObjectEnd();
      }

      @Override
      public ObjectIntMap read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        ObjectIntMap result = type == ObjectIntMap.class ? new ObjectIntMap() : (ObjectIntMap)newInstance(type);
        for (JsonValue child=jsonData.child; child!=null; child=child.next)
          result.put(json.convertFromString(child.name, elementType), child.asInt());
        return result;
      }
    });
    
    setInheritSerializer(IntMap.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, IntMap object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if (knownType == null) knownType = IntMap.class;
        json.writeObjectStart(actualType, knownType);
        for (IntMap.Entry entry : ((IntMap<?>)object).entries())
            json.writeValue(Integer.toString(entry.key), entry.value, elementType);
        json.writeObjectEnd();
      }

      @Override
      public IntMap read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        IntMap result = type == IntMap.class ? new IntMap() : (IntMap)newInstance(type);
        for (JsonValue child=jsonData.child; child!=null; child=child.next)
          result.put(Integer.parseInt(child.name), json.readValue(elementType, null, child));
        return result;
      }
    });
    
    setInheritSerializer(ArrayMap.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, ArrayMap object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if(knownType == null) knownType = ArrayMap.class;
        json.writeObjectStart(actualType, knownType);
        ArrayMap map = (ArrayMap)object;
        for (int i=0, n=map.size; i<n; i++)
          json.writeValue(json.convertToString(map.keys[i], keyType), map.values[i], elementType);
        json.writeObjectEnd();
      }

      @Override
      public ArrayMap read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        ArrayMap result = type == ArrayMap.class ? new ArrayMap() : (ArrayMap)newInstance(type);
        for(JsonValue child = jsonData.child; child != null; child = child.next)
          result.put(json.convertFromString(child.name, keyType), json.readValue(elementType, null, child));
        return result;
      }
    });
    
    setInheritSerializer(Map.class, new JsonSerializer<>() {
      @Override
      public void write(Json2 json, Map object, Class knownType, Class elementType, Class keyType) {
        Class actualType = object.getClass();
        if (actualType.isAnonymousClass()) actualType = actualType.getSuperclass();
        
        if(knownType == null) knownType = java.util.HashMap.class;
        json.writeObjectStart(actualType, knownType);
        for (Map.Entry entry : ((Map<?, ?>)object).entrySet())
          json.writeValue(json.convertToString(entry.getKey(), keyType), entry.getValue(), elementType);
        json.writeObjectEnd();
      }

      @Override
      public Map read(Json2 json, JsonValue jsonData, Class type, Class elementType, Class keyType) {
        Map result = type == ObjectMap.class ? new java.util.HashMap() : (Map)newInstance(type);
        for(JsonValue child = jsonData.child; child != null; child = child.next)
          result.put(json.convertFromString(child.name, keyType), json.readValue(elementType, null, child));
        return result;
      }
    });
  }
}
