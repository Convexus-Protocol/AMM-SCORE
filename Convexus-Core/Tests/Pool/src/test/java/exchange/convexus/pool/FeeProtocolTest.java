/*
 * Copyright 2022 ICONation
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
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.librairies.Position;
import exchange.convexus.librairies.Positions;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.IntUtils;

public class FeeProtocolTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[LOW];
  int tickSpacing = TICK_SPACINGS[LOW];

  int minTick = getMinTick(tickSpacing);
  int maxTick = getMaxTick(tickSpacing);

  BigInteger liquidityAmount = expandTo18Decimals(1000);

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
    mint(alice, minTick, maxTick, liquidityAmount, "999999999999999999946", "999999999999999999946");
  }

  @Test
  void testInitiallySetTo0 () {
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(slot0.feeProtocol, 0);
  }

  @Test
  void testCanBeChangedByTheOwner () {
    pool.invoke(owner, "setFeeProtocol", 6, 6);
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(slot0.feeProtocol, 102);
  }

  @Test
  void testCannotBeChangedOutOfBounds1 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      pool.invoke(owner, "setFeeProtocol", 3, 3), 
      "setFeeProtocol: Bad fees amount");
  }

  @Test
  void testCannotBeChangedOutOfBounds2 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      pool.invoke(owner, "setFeeProtocol", 11, 11), 
      "setFeeProtocol: Bad fees amount");
  }

  @Test
  void testCannotBeChangedNotOwner () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      pool.invoke(alice, "setFeeProtocol", 6, 6), 
      "checkCallerIsFactoryOwner: Only owner can call this method");
  }

  @Test
  void testPositionOwnerGetsFullFeesWhenProtocolFeeIsOff () {
    var fees = swapAndGetFeesOwed(minTick, maxTick, expandTo18Decimals(1), true, true);
    
    // 6 bips * 1e18
    assertEquals(new BigInteger("499999999999999"), fees.token0Fees);
    assertEquals(new BigInteger("0"), fees.token1Fees);
  }

  @Test
  void testSwapFeesAccumulateAsExpected0For1 () {
    doSwap(minTick, maxTick, expandTo18Decimals(1), true, true);
    var position = Position.Info.fromMap(pool.call("positions", Positions.getKey(alice.getAddress(), minTick, maxTick)));
    assertEquals(new BigInteger("499999999999999"), position.tokensOwed0);
    assertEquals(new BigInteger("0"), position.tokensOwed1);

    doSwap(minTick, maxTick, expandTo18Decimals(1), true, true);
    position = Position.Info.fromMap(pool.call("positions", Positions.getKey(alice.getAddress(), minTick, maxTick)));
    assertEquals(new BigInteger("999999999999998"), position.tokensOwed0);
    assertEquals(new BigInteger("0"), position.tokensOwed1);

    doSwap(minTick, maxTick, expandTo18Decimals(1), true, true);
    position = Position.Info.fromMap(pool.call("positions", Positions.getKey(alice.getAddress(), minTick, maxTick)));
    assertEquals(new BigInteger("1499999999999997"), position.tokensOwed0);
    assertEquals(new BigInteger("0"), position.tokensOwed1);
  }

  @Test
  void testSwapFeesAccumulateAsExpected1For0 () {
    doSwap(minTick, maxTick, expandTo18Decimals(1), false, true);
    var position = Position.Info.fromMap(pool.call("positions", Positions.getKey(alice.getAddress(), minTick, maxTick)));
    assertEquals(new BigInteger("0"), position.tokensOwed0);
    assertEquals(new BigInteger("499999999999999"), position.tokensOwed1);

    doSwap(minTick, maxTick, expandTo18Decimals(1), false, true);
    position = Position.Info.fromMap(pool.call("positions", Positions.getKey(alice.getAddress(), minTick, maxTick)));
    assertEquals(new BigInteger("0"), position.tokensOwed0);
    assertEquals(new BigInteger("999999999999998"), position.tokensOwed1);

    doSwap(minTick, maxTick, expandTo18Decimals(1), false, true);
    position = Position.Info.fromMap(pool.call("positions", Positions.getKey(alice.getAddress(), minTick, maxTick)));
    assertEquals(new BigInteger("0"), position.tokensOwed0);
    assertEquals(new BigInteger("1499999999999997"), position.tokensOwed1);
  }

  @Test
  void testPositionOwnerGetsPartialFeesWhenProtocolFeeIsOn () {
    pool.invoke(owner, "setFeeProtocol", 6, 6);
    
    var fees = swapAndGetFeesOwed(minTick, maxTick, expandTo18Decimals(1), true, true);
    
    assertEquals(new BigInteger("416666666666666"), fees.token0Fees);
    assertEquals(new BigInteger("0"), fees.token1Fees);
  }

  @Test
  void testFeesCollectedByLpAfterTwoSwapsShouldBeDoubleOneSwap () {
    doSwap(minTick, maxTick, expandTo18Decimals(1), true, true);
    var fees = swapAndGetFeesOwed(minTick, maxTick, expandTo18Decimals(1), true, true);

    assertEquals(fees.token0Fees, new BigInteger("999999999999998"));
    assertEquals(fees.token1Fees, ZERO);
  }

  @Test
  void testFeesCollectedAfterTwoSwapsWithFeeTurnedOnInMiddleAreFeesFromLastSwap () {
    doSwap(minTick, maxTick, expandTo18Decimals(1), true, false);
    pool.invoke(owner, "setFeeProtocol", 6, 6);
    
    var fees = swapAndGetFeesOwed(minTick, maxTick, expandTo18Decimals(1), true, true);
    
    assertEquals(fees.token0Fees, new BigInteger("916666666666666"));
    assertEquals(fees.token1Fees, ZERO);
  }

  @Test
  void testFeesCollectedByLpAfterTwoSwapsWithIntermediateWithdrawal () {
    pool.invoke(owner, "setFeeProtocol", 6, 6);

    var fees = swapAndGetFeesOwed(minTick, maxTick, expandTo18Decimals(1), true, true);
    
    assertEquals(fees.token0Fees, new BigInteger("416666666666666"));
    assertEquals(fees.token1Fees, ZERO);

    fees = swapAndGetFeesOwed(minTick, maxTick, expandTo18Decimals(1), true, false);
    
    assertEquals(fees.token0Fees, ZERO);
    assertEquals(fees.token1Fees, ZERO);

    var protocolFees = ProtocolFees.fromMap(pool.call("protocolFees"));
    assertEquals(new BigInteger("166666666666666"), protocolFees.token0);
    assertEquals(ZERO, protocolFees.token1);

    burn(minTick, maxTick, ZERO); // poke to update fees

    reset(sicx.spy);
    pool.invoke(alice, "collect", alice.getAddress(), minTick, maxTick, IntUtils.MAX_UINT128, IntUtils.MAX_UINT128);
    verify(sicx.spy).Transfer(pool.getAddress(), alice.getAddress(), new BigInteger ("416666666666666"), "{\"method\": \"pay\"}".getBytes());

    protocolFees = ProtocolFees.fromMap(pool.call("protocolFees"));
    assertEquals(new BigInteger("166666666666666"), protocolFees.token0);
    assertEquals(ZERO, protocolFees.token1);
  }
}