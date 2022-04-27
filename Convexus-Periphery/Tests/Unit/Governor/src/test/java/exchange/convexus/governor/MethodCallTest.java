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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.governor.mocks.MockCaller;
import exchange.convexus.governor.mocks.MockScore;
import exchange.convexus.utils.AddressUtils;
import exchange.convexus.utils.ScoreSpy;
import score.Address;
import scorex.util.HashMap;

public class MethodCallTest extends ConvexusGovernorTest {

  ScoreSpy<MockScore> score;
  ScoreSpy<MockCaller> caller;
  
  @BeforeEach
  void setup() throws Exception {
    score = deploy(MockScore.class);
    caller = deploy(MockCaller.class);
  }

  private void invoke (Account from, BigInteger value, String method, MethodParam[] params) {
    caller.invoke(from, "invoke", score.getAddress(), value, method, params);
  }

  private void invoke (Account from, String method, MethodParam[] params) {
    caller.invoke(from, "invoke", score.getAddress(), BigInteger.ZERO, method, params);
  }

  private Object call (String method, MethodParam[] params) {
    return caller.call("call", score.getAddress(), method, params);
  }

  @Test
  void testCallBigInteger () {
    MethodParam param = new MethodParam("BigInteger", BigInteger.ONE);
    assertEquals(BigInteger.ONE, call("methodBigInteger", new MethodParam[] {param}));
  }
  
  @Test
  void testInvokeBigInteger () {
    MethodParam param = new MethodParam("BigInteger", BigInteger.ONE);
    invoke(owner, "methodBigInteger", new MethodParam[] {param});
  }

  @Test
  void testCallBigIntegerArray () {
    MethodParam param = new MethodParam("Array", "BigInteger", new BigInteger[] {BigInteger.ONE});
    assertEquals(BigInteger.ONE, call("methodBigIntegerArray", new MethodParam[] {param}));
  }

  @Test
  void testCallStringArray () {
    MethodParam param = new MethodParam("Array", "String", new String[] {"A", "B", "C"});
    assertEquals(BigInteger.ONE, call("methodStringArray", new MethodParam[] {param}));
  }

  @Test
  void testInvokePayable () {
    MethodParam param = new MethodParam("BigInteger", BigInteger.ONE);
    sm.transferIcx(owner, caller.getAddress(), BigInteger.ONE);
    invoke(owner, BigInteger.ONE, "methodPayable", new MethodParam[] {param});
  }

  @Test
  void testStruct () {
    var map = new HashMap<String, MethodParam>();
    map.put("a", new MethodParam("BigInteger", BigInteger.ZERO));
    map.put("b", new MethodParam("String", "myString"));
    map.put("c", new MethodParam("Array", "Address", new Address[] {AddressUtils.ZERO_ADDRESS}));
    
    // Hashmap conversion to SCORE type is not supported by javaee-unittest
    // MethodParam param = new MethodParam("Map", map);
    // invoke(owner, "methodStruct", new MethodParam[] {param});
  }
}
