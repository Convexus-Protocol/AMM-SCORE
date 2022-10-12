/*
 * Copyright 2022 Convexus Protocol
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package exchange.convexus.governor;

import java.math.BigInteger;

import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.HashMap;

public class MethodParam {
  // the parameter runtime type
  public String type;
  // for complex types, the runtime subtype 
  public String subtype;
  // the runtime value
  public Object value;

  public MethodParam (
    String type,
    String subtype,
    Object value
  ) {
    this.type = type;
    this.subtype = subtype;
    this.value = value;
  }

  public MethodParam (
    String type,
    Object value
  ) {
    this.type = type;
    this.subtype = null;
    this.value = value;
  }

  @SuppressWarnings("unchecked")
  public Object convert () {
    switch (this.type) {

      // native types
      case "int":
      case "long":
      case "short":
      case "char":
      case "byte":
      case "boolean":
      case "byte[]":
        return this.value;

      // native objects
      case "BigInteger":
      case "String":
      case "Address":
        return this.value;

      // Complex types
      case "Array": {
        switch (this.subtype) {
          case "int":
          case "long":
          case "short":
          case "char":
          case "byte":
          case "boolean":
          case "byte[]":
          case "BigInteger":
          case "String":
          case "Address":
            return this.value;
          case "Map":
            HashMap<String, MethodParam>[] items = (HashMap<String, MethodParam>[]) this.value;
            int arrayLength = items.length;
            HashMap<String, Object>[] array = new HashMap[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
              array[i] = new HashMap<String, Object>();
              for (var entry : items[i].entrySet()) {
                array[i].put(entry.getKey(), entry.getValue());
              }
            }
            return array;
          default:
            Context.revert("MethodParam::convert: Invalid subtype `" + this.subtype + "`");
            return null; // unreachable during runtime but makes the compiler happy
        }
      }

      case "Map": {
        HashMap<String, MethodParam> mapValues = (HashMap<String, MethodParam>) this.value;
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (var entry : mapValues.entrySet()) {
          map.put(entry.getKey(), entry.getValue().convert());
        }
        return map;
      }

      default:
        Context.revert("MethodParam::convert: Invalid type `" + this.type + "`");
        return null; // unreachable during runtime but makes the compiler happy
    }
  }

  public static Object readValue (String type, String subtype, ObjectReader r) {
    switch (type) {
      // native types
      case "int":         return r.readInt();
      case "long":        return r.readLong();
      case "short":       return r.readShort();
      case "char":        return r.readChar();
      case "byte":        return r.readByte();
      case "boolean":     return r.readBoolean();
      case "byte[]":      return r.readByteArray();
      // native objects
      case "BigInteger":  return r.readBigInteger();
      case "String":      return r.readString();
      case "Address":     return r.readAddress();

      // complex types
      case "Array": {
        int arrayLength = r.readInt();
        Object[] items = (Object[]) MethodParam.newArray(subtype, arrayLength);
        r.beginList();
        for (int i = 0; i < arrayLength; i++) {
          items[i] = readValue(subtype, null, r);
        }
        r.end();
        return items;
      }

      case "Map": {
        int mapLength = r.readInt();
        HashMap<String, MethodParam> map = new HashMap<String, MethodParam>();
        r.beginMap();
        for (int i = 0; i < mapLength; i++) {
          String key = r.readString();
          map.put(key, r.read(MethodParam.class));
        }
        r.end();
        return map;
      }

      default:
        Context.revert("MethodParam::readObject: Invalid type `" + type + "`");
        return null; // unreachable during runtime but makes the compiler happy
    }
  }

  private static Object[] newArray (String type, int length) {
    switch (type) {
      // native types
      case "int":           return new Integer[length];
      case "long":          return new Long[length];
      case "short":         return new Short[length];
      case "char":          return new Character[length];
      case "byte":          return new Byte[length];
      case "boolean":       return new Boolean[length];
      case "byte[]":        return new Byte[length][];
      // native objects
      case "BigInteger":    return new BigInteger[length];
      case "String":        return new String[length];
      case "Address":       return new Address[length];
      // complex types
      case "Map":           return new HashMap[length];

      default:
        Context.revert("MethodParam::newArray: Invalid type `" + type + "`");
        return null; // unreachable during runtime but makes the compiler happy
    }
  }

  @SuppressWarnings("unchecked")
  public static void writeValue (ObjectWriter w, String type, String subtype, Object value) {
    switch (type) {
      // native types
      case "int":         w.write((int) value); break;
      case "long":        w.write((long) value); break;
      case "short":       w.write((short) value); break;
      case "char":        w.write((char) value); break;
      case "byte":        w.write((byte) value); break;
      case "boolean":     w.write((boolean) value); break;
      case "byte[]":      w.write((byte[]) value); break;
      // native objects
      case "BigInteger":  w.write((BigInteger) value); break;
      case "String":      w.write((String) value); break;
      case "Address":     w.write((Address) value); break;

      // complex types
      case "Array": {
        Object[] items = (Object[]) value;
        int arrayLength = items.length;
        w.write(arrayLength);
        w.beginList(arrayLength);
        for (int i = 0; i < arrayLength; i++) {
          writeValue(w, subtype, null, items[i]);
        }
        w.end();
        break;
      }

      case "Map": {
        HashMap<String, MethodParam> map = (HashMap<String, MethodParam>) value;
        int mapLength = map.size();
        w.write(mapLength);
        w.beginList(mapLength);
        for (var entry : map.entrySet()) {
          w.write(entry.getKey());
          MethodParam.writeObject(w, entry.getValue());
        }
        w.end();
        break;
      }

      default:
        Context.revert("MethodParam::readObject: Invalid type `" + type + "`");
        break; // unreachable during runtime but makes the compiler happy
    }
  }

  public String toString () {
    return this.type + "/" + this.subtype + "/" + this.value;
  }

  public static MethodParam readObject (ObjectReader r) {
    String type = r.readString();
    String subtype = r.readNullable(String.class);
    Object value = readValue(type, subtype, r);
    return new MethodParam(type, subtype, value);
  }

  public static void writeObject (ObjectWriter w, MethodParam obj) {
    w.write(obj.type);
    w.writeNullable(obj.subtype);
    writeValue(w, obj.type, obj.subtype, obj.value);
  }
}
