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
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.AssertUtils;

public class CollectProtocolTest extends ConvexusPoolTest {

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

    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE);
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    mint(alice, minTick, maxTick, liquidityAmount, "999999999999999999946", "999999999999999999946");
  }

  @Test
  void testOnlyFactoryOwner () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      collectProtocolGetFeesOwed(bob, alice), 
      "onlyFactoryOwner: Only owner can call this method");
  }

  @Test
  void testReturns0IfNoFees () {
    pool.invoke(owner, "setFeeProtocol", 6, 6);

    var fees = collectProtocolGetFeesOwed(owner, alice);

    assertEquals(fees.token0Fees, ZERO);
    assertEquals(fees.token1Fees, ZERO);
  }

  @Test
  void testCanCollectFees () {
    pool.invoke(owner, "setFeeProtocol", 6, 6);

    doSwap(minTick, maxTick, expandTo18Decimals(1), true, true);

    reset(sicx.spy);
    reset(usdc.spy);
    collectProtocolGetFeesOwed(owner, alice);
    verify(sicx.spy).Transfer(pool.getAddress(), alice.getAddress(), new BigInteger ("83333333333332"), "{\"method\": \"pay\"}".getBytes());
    verifyNoInteractions(usdc.spy);
  }

  @Test
  void testFeesCollectedCanDifferBetweenToken0AndToken1 () {
    pool.invoke(owner, "setFeeProtocol", 8, 5);
    
    doSwap(minTick, maxTick, expandTo18Decimals(1), true, false);
    doSwap(minTick, maxTick, expandTo18Decimals(1), false, false);
    
    reset(sicx.spy);
    reset(usdc.spy);
    collectProtocolGetFeesOwed(owner, alice);
    // more token0 fees because it's 1/5th the swap fees
    verify(sicx.spy).Transfer(pool.getAddress(), alice.getAddress(), new BigInteger ("62499999999999"), "{\"method\": \"pay\"}".getBytes());
    // less token1 fees because it's 1/8th the swap fees
    verify(usdc.spy).Transfer(pool.getAddress(), alice.getAddress(), new BigInteger ("99999999999998"), "{\"method\": \"pay\"}".getBytes());
  }
}