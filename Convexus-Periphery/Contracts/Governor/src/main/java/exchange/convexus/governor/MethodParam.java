package exchange.convexus.governor;

import java.math.BigInteger;

import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.HashMap;

public class MethodParam {
  String type;
  Object value;

  public MethodParam (
    String type,
    Object value
  ) {
    this.type = type;
    this.value = value;
  }

  @SuppressWarnings("unchecked")
  public Object convert () {
    switch (this.type) {
      case "int":
      case "string":
      case "boolean":
      case "address":
      case "bytes":
        return this.value;

      case "array": {
        MethodParam[] items = (MethodParam[]) this.value;
        int arrayLength = items.length;
        Object[] array = new Object[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
          array[i] = items[i].convert();
        }
        return array;
      }

      case "map": {
        HashMap<String, MethodParam> mapValues = (HashMap<String, MethodParam>) this.value;
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (var entry : mapValues.entrySet()) {
          map.put(entry.getKey(), entry.getValue().convert());
        }
        return map;
      }

      default:
        Context.revert("MethodParam::readObject: Invalid method parameter type");
        return null; // unreachable during runtime but makes the compiler happy
    }
  }

  public static MethodParam readObject (ObjectReader r) {
    String type = r.readString();

    switch (type) {
      case "int":
        return new MethodParam(type, r.readBigInteger());

      case "string":
        return new MethodParam(type, r.readString());

      case "boolean":
        return new MethodParam(type, r.readBoolean());

      case "address":
        return new MethodParam(type, r.readAddress());

      case "bytes":
        return new MethodParam(type, r.readByteArray());

      case "array": {
        int arrayLength = r.readInt();
        MethodParam[] items = new MethodParam[arrayLength];
        r.beginList();
        for (int i = 0; i < arrayLength; i++) {
          items[i] = r.read(MethodParam.class);
        }
        r.end();
        return new MethodParam(type, items);
      }

      case "map": {
        int mapLength = r.readInt();
        HashMap<String, MethodParam> map = new HashMap<String, MethodParam>();
        r.beginMap();
        for (int i = 0; i < mapLength; i++) {
          String key = r.readString();
          map.put(key, r.read(MethodParam.class));
        }
        r.end();
        return new MethodParam(type, map);
      }

      default:
        Context.revert("MethodParam::readObject: Invalid method parameter type");
        return null; // unreachable during runtime but makes the compiler happy
    }
  }

  @SuppressWarnings("unchecked")
  public static void writeObject (ObjectWriter w, MethodParam obj) {
    w.write(obj.type);
    
    switch (obj.type) {
      case "int":
        w.write((BigInteger) obj.value);
      break;

      case "string":
        w.write((String) obj.value);
      break;

      case "boolean":
        w.write((boolean) obj.value);
      break;

      case "address":
        w.write((Address) obj.value);
      break;

      case "bytes":
        w.write((byte[]) obj.value);
      break;

      case "array": {
        MethodParam[] items = (MethodParam[]) obj.value;
        int arrayLength = items.length;
        w.write(arrayLength);
        w.beginList(arrayLength);
        for (int i = 0; i < arrayLength; i++) {
          w.write(items[i]);
        }
        w.end();
        break;
      }

      case "map": {
        HashMap<String, MethodParam> map = (HashMap<String, MethodParam>) obj.value;
        int mapLength = map.size();
        w.write(mapLength);
        w.beginList(mapLength);
        for (var entry : map.entrySet()) {
          w.write(entry.getKey());
          w.write(entry.getValue());
        }
        w.end();
        break;
      }

      default:
        Context.revert("MethodParam::readObject: Invalid method parameter type");
        break; // unreachable during runtime but makes the compiler happy
    }
  }
}
