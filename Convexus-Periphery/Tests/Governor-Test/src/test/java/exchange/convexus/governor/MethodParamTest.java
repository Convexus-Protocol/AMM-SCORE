package exchange.convexus.governor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.utils.AddressUtils;
import score.Address;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import scorex.util.HashMap;

public class MethodParamTest extends ConvexusGovernorTest {
  
  @BeforeEach
  void setup() throws Exception {
  }

  @Test
  void testInt () {
    var value = BigInteger.ONE;
    MethodParam param = new MethodParam("int", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);
    assertEquals(value, result.value);
    assertEquals(value, result.convert());
  }

  @Test
  void testString () {
    var value = "myString";
    MethodParam param = new MethodParam("string", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);
    assertEquals(value, result.value);
    assertEquals(value, result.convert());
  }

  @Test
  void testBoolean () {
    var value = true;
    MethodParam param = new MethodParam("boolean", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);
    assertEquals(value, result.value);
    assertEquals(value, result.convert());
  }

  @Test
  void testAddress () {
    var value = AddressUtils.ZERO_ADDRESS;
    MethodParam param = new MethodParam("address", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);
    assertEquals(value, result.value);
    assertEquals(value, result.convert());
  }

  @Test
  void testBytes () {
    var value = new byte[]{(byte) 0x1, (byte) 0x2, (byte) 0x3};
    MethodParam param = new MethodParam("bytes", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);
    assertArrayEquals(value, (byte[]) result.value);
    assertArrayEquals(value, (byte[]) result.convert());
  }

  @Test
  void testArray () {
    var value = new MethodParam[] {
      new MethodParam("address", AddressUtils.ZERO_ADDRESS),
      new MethodParam("int", BigInteger.ONE),
      new MethodParam("string", "myString")
    };
    MethodParam param = new MethodParam("array", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);

    MethodParam[] resultValues = (MethodParam[]) result.value;
    assertEquals((Address) value[0].value, (Address) resultValues[0].value);
    assertEquals((BigInteger) value[1].value, (BigInteger) resultValues[1].value);
    assertEquals((String) value[2].value, (String) resultValues[2].value);
    
    Object[] converted = (Object[]) result.convert();
    assertEquals((Address) value[0].value, (Address) converted[0]);
    assertEquals((BigInteger) value[1].value, (BigInteger) converted[1]);
    assertEquals((String) value[2].value, (String) converted[2]);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMap () {
    var value = new HashMap<String, MethodParam>();
    value.put("key1", new MethodParam("address", AddressUtils.ZERO_ADDRESS));
    value.put("key2", new MethodParam("int", BigInteger.ONE));
    value.put("key3", new MethodParam("string", "myString"));

    MethodParam param = new MethodParam("map", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);

    HashMap<String, MethodParam> resultValues = (HashMap<String, MethodParam>) result.value;
    assertEquals((Address) value.get("key1").value, (Address) resultValues.get("key1").value);
    assertEquals((BigInteger) value.get("key2").value, (BigInteger) resultValues.get("key2").value);
    assertEquals((String) value.get("key3").value, (String) resultValues.get("key3").value);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testNested () {
    var map = new HashMap<String, MethodParam>();
    map.put("subkey1", new MethodParam("address", AddressUtils.ZERO_ADDRESS));
    map.put("subkey2", new MethodParam("int", BigInteger.ONE));
    map.put("subkey3", new MethodParam("string", "myString"));

    var value = new HashMap<String, MethodParam>();
    value.put("key1", new MethodParam("address", AddressUtils.ZERO_ADDRESS));
    value.put("key2", new MethodParam("array", new MethodParam[] {
      new MethodParam("address", AddressUtils.ZERO_ADDRESS),
      new MethodParam("int", BigInteger.ONE),
      new MethodParam("map", map)
    }));
    value.put("key3", new MethodParam("string", "myString"));

    MethodParam param = new MethodParam("map", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);

    var converted = (HashMap<String, Object>) result.convert();

    // check key 1
    HashMap<String, MethodParam> resultValues = (HashMap<String, MethodParam>) result.value;
    assertEquals((Address) value.get("key1").value, (Address) resultValues.get("key1").value);
    assertEquals((Address) converted.get("key1"), (Address) resultValues.get("key1").value);

    // check key 2
    MethodParam[] resultArrayValues = (MethodParam[]) resultValues.get("key2").value;
    MethodParam[] originArrayValues = (MethodParam[]) value.get("key2").value;
    Object[] convertedArrayValues = (Object[]) converted.get("key2");
    
    assertEquals((Address) originArrayValues[0].value, (Address) resultArrayValues[0].value);
    assertEquals((BigInteger) originArrayValues[1].value, (BigInteger) resultArrayValues[1].value);
    assertEquals((Address) convertedArrayValues[0], (Address) resultArrayValues[0].value);
    assertEquals((BigInteger) convertedArrayValues[1], (BigInteger) resultArrayValues[1].value);
    
    HashMap<String, MethodParam> resultSubkey3 = (HashMap<String, MethodParam>) resultArrayValues[2].value;
    HashMap<String, MethodParam> originSubkey3 = (HashMap<String, MethodParam>) originArrayValues[2].value;
    HashMap<String, Object> convertSubkey3 = (HashMap<String, Object>) convertedArrayValues[2];
    assertEquals((Address) originSubkey3.get("subkey1").value, (Address) resultSubkey3.get("subkey1").value);
    assertEquals((BigInteger) originSubkey3.get("subkey2").value, (BigInteger) resultSubkey3.get("subkey2").value);
    assertEquals((String) originSubkey3.get("subkey3").value, (String) resultSubkey3.get("subkey3").value);
    assertEquals((Address) convertSubkey3.get("subkey1"), (Address) resultSubkey3.get("subkey1").value);
    assertEquals((BigInteger) convertSubkey3.get("subkey2"), (BigInteger) resultSubkey3.get("subkey2").value);
    assertEquals((String) convertSubkey3.get("subkey3"), (String) resultSubkey3.get("subkey3").value);

    // check key 3
    assertEquals((String) value.get("key3").value, (String) resultValues.get("key3").value);
    assertEquals((String) converted.get("key3"), (String) resultValues.get("key3").value);
  }
}
