package exchange.convexus.governor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.structs.MethodParam;
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
  void testBigInteger () {
    var value = BigInteger.ONE;
    MethodParam param = new MethodParam("BigInteger", value);
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
    MethodParam param = new MethodParam("String", value);
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
    MethodParam param = new MethodParam("Address", value);
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
    MethodParam param = new MethodParam("byte[]", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);
    assertArrayEquals(value, (byte[]) result.value);
    assertArrayEquals(value, (byte[]) result.convert());
  }

  @Test
  void testArray () {
    var value = new BigInteger[] {
      BigInteger.valueOf(1),
      BigInteger.valueOf(2),
      BigInteger.valueOf(3)
    };
    MethodParam param = new MethodParam("Array", "BigInteger", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);

    BigInteger[] resultValues = (BigInteger[]) result.value;
    assertEquals((BigInteger) value[0], (BigInteger) resultValues[0]);
    assertEquals((BigInteger) value[1], (BigInteger) resultValues[1]);
    assertEquals((BigInteger) value[2], (BigInteger) resultValues[2]);
    
    BigInteger[] converted = (BigInteger[]) result.convert();
    assertEquals((BigInteger) value[0], (BigInteger) converted[0]);
    assertEquals((BigInteger) value[1], (BigInteger) converted[1]);
    assertEquals((BigInteger) value[2], (BigInteger) converted[2]);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMap () {
    var value = new HashMap<String, MethodParam>();
    value.put("key1", new MethodParam("Address", AddressUtils.ZERO_ADDRESS));
    value.put("key2", new MethodParam("BigInteger", BigInteger.ONE));
    value.put("key3", new MethodParam("String", "myString"));

    MethodParam param = new MethodParam("Map", value);
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

    var value = new HashMap<String, MethodParam>();

    var map1 = new HashMap<String, MethodParam>();
    map1.put("subkeymap1", new MethodParam("BigInteger", BigInteger.valueOf(1337)));
    var map2 = new HashMap<String, MethodParam>();
    map2.put("subkeymap2", new MethodParam("BigInteger", BigInteger.valueOf(1337)));
    var map3 = new HashMap<String, MethodParam>();
    map3.put("subkeymap3", new MethodParam("BigInteger", BigInteger.valueOf(1337)));

    value.put("key1", new MethodParam("Address", AddressUtils.ZERO_ADDRESS));
    value.put("key2", new MethodParam("Array", "Map", new HashMap[] { map1, map2, map3 }));
    value.put("key3", new MethodParam("String", "myString"));

    MethodParam param = new MethodParam("Map", value);
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    MethodParam.writeObject(writer, param);

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", writer.toByteArray());
    MethodParam result = MethodParam.readObject(reader);

    var converted = (HashMap<String, Object>) param.convert();
    HashMap<String, MethodParam> resultValues = (HashMap<String, MethodParam>) result.value;

    // check key 1
    assertEquals((Address) value.get("key1").value, (Address) resultValues.get("key1").value);
    assertEquals((Address) converted.get("key1"), (Address) resultValues.get("key1").value);

    // check key 2
    var valueKey2 = (HashMap<String, MethodParam>[]) value.get("key2").value;
    var resultKey2 = (HashMap<String, MethodParam>[]) resultValues.get("key2").value;
    var converted2 = (HashMap<String, MethodParam>[]) value.get("key2").convert();
    assertEquals(valueKey2[0].get("subkeymap1").value, resultKey2[0].get("subkeymap1").value);
    assertEquals(valueKey2[1].get("subkeymap2").value, resultKey2[1].get("subkeymap2").value);
    assertEquals(valueKey2[2].get("subkeymap3").value, resultKey2[2].get("subkeymap3").value);

    assertEquals(valueKey2[0].get("subkeymap1").value, converted2[0].get("subkeymap1").value);
    assertEquals(valueKey2[1].get("subkeymap2").value, converted2[1].get("subkeymap2").value);
    assertEquals(valueKey2[2].get("subkeymap3").value, converted2[2].get("subkeymap3").value);

    // check key 3
    assertEquals((String) value.get("key3").value, (String) resultValues.get("key3").value);
    assertEquals((String) converted.get("key3"), (String) resultValues.get("key3").value);
  }
}
