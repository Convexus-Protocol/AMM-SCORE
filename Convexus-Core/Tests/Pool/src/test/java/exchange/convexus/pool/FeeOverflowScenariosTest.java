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

import static exchange.convexus.utils.IntUtils.MAX_UINT128;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;

public class FeeOverflowScenariosTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;

  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];
  int tickLower = -TICK_SPACINGS[MEDIUM];
  int tickUpper = TICK_SPACINGS[MEDIUM];

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
  }

  @Test
  void testUpToMaxUint128 () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    mint(alice, minTick, maxTick, BigInteger.valueOf(1), "1", "1");
    flash(alice, ZERO, ZERO, alice.getAddress(), MAX_UINT128, MAX_UINT128);

    BigInteger feeGrowthGlobal0X128 = (BigInteger) pool.call("feeGrowthGlobal0X128");
    BigInteger feeGrowthGlobal1X128 = (BigInteger) pool.call("feeGrowthGlobal1X128");
    
    // all 1s in first 128 bits
    assertEquals(MAX_UINT128.shiftLeft(128), feeGrowthGlobal0X128);
    assertEquals(MAX_UINT128.shiftLeft(128), feeGrowthGlobal1X128);

    burn(minTick, maxTick, ZERO);

    var fees = collectGetFeesOwed(minTick, maxTick);
    assertEquals(MAX_UINT128, fees.token0Fees);
    assertEquals(MAX_UINT128, fees.token0Fees);
  }

  @Test
  void testOverflowMaxUint128 () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    mint(alice, minTick, maxTick, BigInteger.valueOf(1), "1", "1");
    flash(alice, ZERO, ZERO, alice.getAddress(), MAX_UINT128, MAX_UINT128);
    flash(alice, ZERO, ZERO, alice.getAddress(), ONE, ONE);

    BigInteger feeGrowthGlobal0X128 = (BigInteger) pool.call("feeGrowthGlobal0X128");
    BigInteger feeGrowthGlobal1X128 = (BigInteger) pool.call("feeGrowthGlobal1X128");
    
    // all 1s in first 128 bits
    assertEquals(ZERO, feeGrowthGlobal0X128);
    assertEquals(ZERO, feeGrowthGlobal1X128);
    burn(minTick, maxTick, ZERO);

      // fees burned
    var fees = collectGetFeesOwed(minTick, maxTick);
    assertEquals(ZERO, fees.token0Fees);
    assertEquals(ZERO, fees.token0Fees);
  }

  @Test
  void testOverflowMaxUint128AfterPokeBurnsFeesOwedTo0 () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    mint(alice, minTick, maxTick, BigInteger.valueOf(1), "1", "1");
    flash(alice, ZERO, ZERO, alice.getAddress(), MAX_UINT128, MAX_UINT128);
    burn(minTick, maxTick, ZERO);
    flash(alice, ZERO, ZERO, alice.getAddress(), ONE, ONE);
    burn(minTick, maxTick, ZERO);

    // fees burned
    var fees = collectGetFeesOwed(minTick, maxTick);
    assertEquals(ZERO, fees.token0Fees);
    assertEquals(ZERO, fees.token0Fees);
  }

  @Test
  void testTwoPositionsAtTheSameSnapshot () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    mint(alice, minTick, maxTick, BigInteger.valueOf(1), "1", "1");
    mint(bob,   minTick, maxTick, BigInteger.valueOf(1), "1", "1");
    
    flash(alice, ZERO, ZERO, alice.getAddress(), MAX_UINT128, ZERO);
    flash(alice, ZERO, ZERO, alice.getAddress(), MAX_UINT128, ZERO);

    var feeGrowthGlobal0X128 = (BigInteger) pool.call("feeGrowthGlobal0X128");
    assertEquals(MAX_UINT128.shiftLeft(128), feeGrowthGlobal0X128);
    
    flash(alice, ZERO, ZERO, alice.getAddress(), TWO, ZERO);
    burn(alice, minTick, maxTick, ZERO);
    burn(bob, minTick, maxTick, ZERO);

    var fees = collectGetFeesOwed(alice, minTick, maxTick);
    assertEquals(ZERO, fees.token0Fees);

    fees = collectGetFeesOwed(bob, minTick, maxTick);
    assertEquals(ZERO, fees.token0Fees);
  }

  @Test
  void twoPositions1FeesApartOverflowsExactlyOnce () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    mint(alice, minTick, maxTick, BigInteger.valueOf(1), "1", "1");
    flash(alice, ZERO, ZERO, alice.getAddress(), ONE, ZERO);
    mint(bob,   minTick, maxTick, BigInteger.valueOf(1), "1", "1");
    flash(alice, ZERO, ZERO, alice.getAddress(), MAX_UINT128, ZERO);
    flash(alice, ZERO, ZERO, alice.getAddress(), MAX_UINT128, ZERO);

    var feeGrowthGlobal0X128 = (BigInteger) pool.call("feeGrowthGlobal0X128");
    assertEquals(ZERO, feeGrowthGlobal0X128);

    flash(alice, ZERO, ZERO, alice.getAddress(), TWO, ZERO);
    burn(alice, minTick, maxTick, ZERO);
    burn(bob, minTick, maxTick, ZERO);
    
    var fees = collectGetFeesOwed(alice, minTick, maxTick);
    assertEquals(ONE, fees.token0Fees);

    fees = collectGetFeesOwed(bob, minTick, maxTick);
    assertEquals(ZERO, fees.token0Fees);
  }
}