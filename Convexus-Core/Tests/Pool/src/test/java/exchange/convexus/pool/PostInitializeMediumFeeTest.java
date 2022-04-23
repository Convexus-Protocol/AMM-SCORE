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
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;

public class PostInitializeMediumFeeTest extends ConvexusPoolTest {

  private static final BigInteger THREE = BigInteger.valueOf(3);
  private static final BigInteger FIVE = BigInteger.valueOf(5);
  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[LOW];
  int tickSpacing = TICK_SPACINGS[LOW];

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
  void testReturns0BeforeInitialization() {
    assertEquals(ZERO, pool.call("liquidity"));
  }

  @Test
  void testReturnsInitialLiquidity () {
    initializeAtZeroTick();
    assertEquals(TEN.pow(18).multiply(TWO), pool.call("liquidity"));
  }

  @Test
  void testReturnsInSupplyInRange () {
    initializeAtZeroTick();
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1499550104979004"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1499550104979004"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -tickSpacing, tickSpacing, TEN.pow(18).multiply(THREE));
    
    assertEquals(TEN.pow(18).multiply(FIVE), pool.call("liquidity"));
  }

  @Test
  void testExcludesSupplyAtTickAboveCurrentTick () {
    initializeAtZeroTick();
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1498800554806557"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), tickSpacing, tickSpacing * 2, TEN.pow(18).multiply(THREE));

    assertEquals(TEN.pow(18).multiply(TWO), pool.call("liquidity"));
  }

  @Test
  void testUpdatesCorrectWhenExitingRange () {
    initializeAtZeroTick();

    BigInteger kBefore = (BigInteger) pool.call("liquidity");
    assertEquals(TEN.pow(18).multiply(TWO), kBefore);

    // add liquidity at and above current tick
    BigInteger liquidityDelta = TEN.pow(18);
    int lowerTick = 0;
    int upperTick = tickSpacing;

    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("499850034993002"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), lowerTick, upperTick, liquidityDelta);
    
    // ensure virtual supply has increased appropriately
    BigInteger kAfter = (BigInteger) pool.call("liquidity");
    assertEquals(TEN.pow(18).multiply(THREE), kAfter);
    
    // swap toward the left (just enough for the tick transition function to trigger)
    swapExact0For1(ONE, alice);
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(-1, slot0.tick);
    
    BigInteger kAfterAfterSwap = (BigInteger) pool.call("liquidity");
    assertEquals(TEN.pow(18).multiply(TWO), kAfterAfterSwap);
  }
  
  @Test
  void testUpdatesCorrectWhenEnteringRange () {
    initializeAtZeroTick();

    BigInteger kBefore = (BigInteger) pool.call("liquidity");
    assertEquals(TEN.pow(18).multiply(TWO), kBefore);

    // add liquidity below the current tick
    BigInteger liquidityDelta = TEN.pow(18);
    int lowerTick = -tickSpacing;
    int upperTick = 0;

    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("499850034993002"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), lowerTick, upperTick, liquidityDelta);
    
    // ensure virtual supply hasn't changed
    BigInteger kAfter = (BigInteger) pool.call("liquidity");
    assertEquals(kBefore, kAfter);
    
    // swap toward the left (just enough for the tick transition function to trigger)
    swapExact0For1(ONE, alice);
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(-1, slot0.tick);
    
    BigInteger kAfterAfterSwap = (BigInteger) pool.call("liquidity");
    assertEquals(TEN.pow(18).multiply(THREE), kAfterAfterSwap);
  }
}