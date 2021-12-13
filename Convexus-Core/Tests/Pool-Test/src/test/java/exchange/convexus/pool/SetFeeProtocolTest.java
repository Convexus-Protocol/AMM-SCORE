/*
 * Copyright 2021 ICONation
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

package exchange.convexus.pool;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.AssertUtils;

public class SetFeeProtocolTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];

  int minTick = getMinTick(tickSpacing);
  int maxTick = getMaxTick(tickSpacing);

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_factory();
    reset(factory.spy);
    setup_tokens();
    setup_pool(factory.getAddress(), FEE, tickSpacing);
    reset(pool.spy);

    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE, pool.getAddress());
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
  }

  @Test
  void testCanOnlyBeCalledByFactoryOwner () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.invoke(bob, "setFeeProtocol", 5, 5), 
      "onlyFactoryOwner: Only owner can call this method");
  }

  @Test
  void testFailsIfFeeIsLt4OrGt10_1 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.invoke(owner, "setFeeProtocol", 3, 3), 
      "setFeeProtocol: Bad fees amount");
  }
  @Test
  void testFailsIfFeeIsLt4OrGt10_2 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.invoke(owner, "setFeeProtocol", 6, 3), 
      "setFeeProtocol: Bad fees amount");
  }
  @Test
  void testFailsIfFeeIsLt4OrGt10_3 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.invoke(owner, "setFeeProtocol", 3, 6), 
      "setFeeProtocol: Bad fees amount");
  }
  @Test
  void testFailsIfFeeIsLt4OrGt10_4 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.invoke(owner, "setFeeProtocol", 11, 11), 
      "setFeeProtocol: Bad fees amount");
  }
  @Test
  void testFailsIfFeeIsLt4OrGt10_5 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.invoke(owner, "setFeeProtocol", 6, 11), 
      "setFeeProtocol: Bad fees amount");
  }
  @Test
  void testFailsIfFeeIsLt4OrGt10_6 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.invoke(owner, "setFeeProtocol", 11, 6), 
      "setFeeProtocol: Bad fees amount");
  }

  @Test
  void testSucceedsForFeeOf4 () {
    pool.invoke(owner, "setFeeProtocol", 4, 4);
  }
  @Test
  void testSucceedsForFeeOf10 () {
    pool.invoke(owner, "setFeeProtocol", 10, 10);
  }

  @Test
  void testSetsProtocolFee () {
    pool.invoke(owner, "setFeeProtocol", 7, 7);
    
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(119, slot0.feeProtocol);
  }

  @Test
  void testCanChangeProtocolFee () {
    pool.invoke(owner, "setFeeProtocol", 7, 7);
    pool.invoke(owner, "setFeeProtocol", 5, 8);

    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(133, slot0.feeProtocol);
  }

  @Test
  void testCanTurnOffProtocolFee () {
    pool.invoke(owner, "setFeeProtocol", 4, 4);
    pool.invoke(owner, "setFeeProtocol", 0, 0);
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(0, slot0.feeProtocol);
  }

  @Test
  void testEmitsAnEventWhenTurnedOn () {
    reset(pool.spy);
    pool.invoke(owner, "setFeeProtocol", 7, 7);
    verify(pool.spy).SetFeeProtocol(0, 0, 7, 7);
  }

  @Test
  void testEmitsAnEventWhenTurnedOff () {
    reset(pool.spy);
    pool.invoke(owner, "setFeeProtocol", 7, 5);
    pool.invoke(owner, "setFeeProtocol", 0, 0);
    verify(pool.spy).SetFeeProtocol(7, 5, 0, 0);
  }

  @Test
  void testEmitsAnEventWhenChanged () {
    reset(pool.spy);
    pool.invoke(owner, "setFeeProtocol", 4, 10);
    pool.invoke(owner, "setFeeProtocol", 6, 8);
    verify(pool.spy).SetFeeProtocol(4, 10, 6, 8);
  }

  @Test
  void testEmitsAnEventWhenUnchanged () {
    reset(pool.spy);
    pool.invoke(owner, "setFeeProtocol", 5, 9);
    pool.invoke(owner, "setFeeProtocol", 5, 9);
    verify(pool.spy).SetFeeProtocol(5, 9, 5, 9);
  }
}
