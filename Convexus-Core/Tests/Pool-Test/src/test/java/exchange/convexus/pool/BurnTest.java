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

import static exchange.convexus.utils.SleepUtils.sleep;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.reset;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.librairies.Position;
import exchange.convexus.librairies.Positions;
import exchange.convexus.librairies.Tick;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;

public class BurnTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];

  int minTick = getMinTick(tickSpacing);
  int maxTick = getMaxTick(tickSpacing);

  private void checkTickIsClear(int tick) {
    var tickInfo = Tick.Info.fromMap(pool.call("ticks", tick));
    assertEquals(ZERO, tickInfo.liquidityGross);
    assertEquals(ZERO, tickInfo.feeGrowthOutside0X128);
    assertEquals(ZERO, tickInfo.feeGrowthOutside1X128);
    assertEquals(ZERO, tickInfo.liquidityNet);
  }

  private void checkTickIsNotClear(int tick) {
    var tickInfo = Tick.Info.fromMap(pool.call("ticks", tick));
    assertNotEquals(ZERO, tickInfo.liquidityGross);
  }

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_factory();
    reset(factory.spy);
    setup_tokens();
    setup_pool(factory.getAddress(), FEE, tickSpacing);
    reset(pool.spy);

    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE);

    // initialize at zero tick
    initializeAtZeroTick();
  }

  @Test
  void testDoesNotClearPositionFeeGrowthSnapshotIfNoMoreLIquidity () {
      // some activity that would make the ticks non-zero
      sleep(10);
      
      ConvexusLiquidityUtils.deposit(bob, callee.getAddress(), sicx.score, new BigInteger("1000000000000000000"));
      ConvexusLiquidityUtils.deposit(bob, callee.getAddress(), usdc.score, new BigInteger("1000000000000000000"));
      callee.invoke(bob, "mint", pool.getAddress(), bob.getAddress(), minTick, maxTick, TEN.pow(18));
      
      ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1000000000000000000"));
      swapExact0For1(TEN.pow(18), alice);
      
      ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1000000000000000000"));
      swapExact1For0(TEN.pow(18), alice);

      pool.invoke(bob, "burn", minTick, maxTick, TEN.pow(18));
      var position = Position.Info.fromMap(pool.call("positions", Positions.getKey(bob.getAddress(), minTick, maxTick)));
      assertEquals(ZERO, position.liquidity);
      assertNotEquals(ZERO, position.tokensOwed0);
      assertNotEquals(ZERO, position.tokensOwed1);
      assertEquals(new BigInteger("340282366920938463463374607431768211"), position.feeGrowthInside0LastX128);
      assertEquals(new BigInteger("340282366920938576890830247744589365"), position.feeGrowthInside1LastX128);
  }

  @Test
  void testClearsTickIfItsLastPosition () {
    int tickLower = minTick + tickSpacing;
    int tickUpper = maxTick - tickSpacing;
    // some activity that would make the ticks non-zero
    sleep(10);
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), tickLower, tickUpper, ONE);
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1000000000000000000"));
    swapExact0For1(TEN.pow(18), alice);
    
    pool.invoke(alice, "burn", tickLower, tickUpper, ONE);
    checkTickIsClear(tickLower);
    checkTickIsClear(tickUpper);
  }

  @Test
  void testClearsOnlyLowerTickIfUpperIsStillUsed () {
    int tickLower = minTick + tickSpacing;
    int tickUpper = maxTick - tickSpacing;

    // some activity that would make the ticks non-zero
    sleep(10);
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), tickLower, tickUpper, ONE);
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), tickLower + tickSpacing, tickUpper, ONE);
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1000000000000000000"));
    swapExact0For1(TEN.pow(18), alice);
    
    pool.invoke(alice, "burn", tickLower, tickUpper, ONE);
    checkTickIsClear(tickLower);
    checkTickIsNotClear(tickUpper);
  }

  @Test
  void testClearsOnlyTheUpperTickIfLowerStillUsed () {
    int tickLower = minTick + tickSpacing;
    int tickUpper = maxTick - tickSpacing;

    // some activity that would make the ticks non-zero
    sleep(10);
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), tickLower, tickUpper, ONE);
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), tickLower, tickUpper - tickSpacing, ONE);
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1000000000000000000"));
    swapExact0For1(TEN.pow(18), alice);
    
    pool.invoke(alice, "burn", tickLower, tickUpper, ONE);
    checkTickIsNotClear(tickLower);
    checkTickIsClear(tickUpper);
  }
}