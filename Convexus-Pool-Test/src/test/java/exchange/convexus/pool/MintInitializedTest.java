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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.librairies.Oracle;
import exchange.convexus.librairies.Position;
import exchange.convexus.librairies.Positions;
import exchange.convexus.librairies.Tick;
import exchange.convexus.liquidity.ConvexusLiquidity;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.TimeUtils;
import score.Address;

public class MintInitializedTest extends ConvexusPoolTest {

  private static final BigInteger HUNDRED = BigInteger.valueOf(100);
  
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
    initializeWithPrice10_1();
  }

  // =========================================
  // ============= failure cases ============= 
  // =========================================
  BigInteger initializeWithPrice10_1() {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, TEN));
    var start = TimeUtils.nowSeconds();
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, BigInteger.valueOf(1000));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, BigInteger.valueOf(9996));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick, maxTick, BigInteger.valueOf(3161));
    return start;
  }
  
  @Test
  void testTickLowerGreaterThanTickUpper () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), 1, 0, ONE),
      "checkTicks: tickLower must be lower than tickUpper");
  }

  @Test
  void testTickLowerLessThanMinTick () {

    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), TickMath.MIN_TICK - 1, 0, ONE),
      "checkTicks: tickLower lower than expected");
  }

  @Test
  void testTickUpperGreaterThanMaxTick () {

    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), 0, TickMath.MAX_TICK + 1, ONE),
      "checkTicks: tickUpper greater than expected");
  }

  @Test
  void testAmountExceedsTheMax () {

    var maxLiquidityGross = (BigInteger) pool.call("maxLiquidityPerTick");

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("36384355944659449090771081304852070"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("3638435594465944908512579814181605"));
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, maxLiquidityGross.add(ONE)),
      "update: liquidityGrossAfter <= maxLiquidity");
  }
  
  @Test
  void testAmountNotExceedsTheMax () {

    var maxLiquidityGross = (BigInteger) pool.call("maxLiquidityPerTick");

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("36384355944659449090771081304852070"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("3638435594465944908512579814181605"));
    
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, maxLiquidityGross);
  }

  @Test
  void testTotalAmountAtTickExceedsMax () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("3163"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("317"));

    BigInteger THOUSAND = BigInteger.valueOf(1000);

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, THOUSAND);
    
    var maxLiquidityGross = (BigInteger) pool.call("maxLiquidityPerTick");

    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, maxLiquidityGross.subtract(THOUSAND).add(ONE)),
      "update: liquidityGrossAfter <= maxLiquidity");
  }

  @Test
  void testTotalAmountAtTickExceedsMax2 () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("3163"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("317"));

    BigInteger THOUSAND = BigInteger.valueOf(1000);

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, THOUSAND);
    
    var maxLiquidityGross = (BigInteger) pool.call("maxLiquidityPerTick");

    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing * 2, maxTick - tickSpacing, maxLiquidityGross.subtract(THOUSAND).add(ONE)),
      "update: liquidityGrossAfter <= maxLiquidity");
  }
  
  @Test
  void testTotalAmountAtTickExceedsMax3 () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("3163"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("317"));

    BigInteger THOUSAND = BigInteger.valueOf(1000);

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, THOUSAND);
    
    var maxLiquidityGross = (BigInteger) pool.call("maxLiquidityPerTick");

    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing * 2, maxLiquidityGross.subtract(THOUSAND).add(ONE)),
      "update: liquidityGrossAfter <= maxLiquidity");
  }
  
  @Test
  void testTotalAmountAtTickExceedsMax4 () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("3163"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("317"));

    BigInteger THOUSAND = BigInteger.valueOf(1000);

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, THOUSAND);
    
    var maxLiquidityGross = (BigInteger) pool.call("maxLiquidityPerTick");

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("36384355944659449090771081304848907"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("3638435594465944908512579814181288"));

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, maxLiquidityGross.subtract(THOUSAND));
  }
  
  @Test
  void testTotalAmountAtTickExceedsMax5 () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("3163"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("317"));

    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, ZERO),
      "mint: amount must be superior to 0");
  }
  
  // =========================================
  // ============= Success cases ============= 
  // =========================================
  @Test
  void testInitialBalances () {
    
    assertEquals(BigInteger.valueOf(9996), sicx.call("balanceOf", pool.getAddress()));
    assertEquals(BigInteger.valueOf(1000), usdc.call("balanceOf", pool.getAddress()));
  }

  @Test
  void testInitialTick () {
    
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(-23028, slot0.tick);
  }

  @Test
  void testToken0Only () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("21549"));

    reset(callee.spy);
    reset(sicx.spy);
    reset(usdc.spy);

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -22980, 0, BigInteger.valueOf(10000));

    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), BigInteger.valueOf(21549), "pay".getBytes());
    verifyNoInteractions(usdc.spy);
    
    assertEquals(BigInteger.valueOf(9996 + 21549), sicx.call("balanceOf", pool.getAddress()));
    assertEquals(BigInteger.valueOf(1000), usdc.call("balanceOf", pool.getAddress()));
  }

  @Test
  void testMaxTickWithMaxLeverage () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("828011525"));

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), maxTick - tickSpacing, maxTick, TWO.pow(102));

    assertEquals(BigInteger.valueOf(9996 + 828011525), sicx.call("balanceOf", pool.getAddress()));
    assertEquals(BigInteger.valueOf(1000), usdc.call("balanceOf", pool.getAddress()));
  }

  @Test
  void testForMaxTick () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("31549"));

    reset(callee.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -22980, maxTick, BigInteger.valueOf(10000));
    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), BigInteger.valueOf(31549), "pay".getBytes());

    assertEquals(BigInteger.valueOf(9996 + 31549), sicx.call("balanceOf", pool.getAddress()));
    assertEquals(BigInteger.valueOf(1000), usdc.call("balanceOf", pool.getAddress()));
  }
  
  @Test
  void testRemovingWorks () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("121"));

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -240, 0, BigInteger.valueOf(10000));
    pool.invoke(alice, "burn", -240, 0, BigInteger.valueOf(10000));

    reset(pool.spy);
    pool.invoke(alice, "collect", alice.getAddress(), -240, 0, IntUtils.MAX_UINT256, IntUtils.MAX_UINT256);

    // Get Collect event
    ArgumentCaptor<Address> _caller = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _tickLower = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickUpper = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(pool.spy).Collect(_caller.capture(), _tickLower.capture(), _tickUpper.capture(), _recipient.capture(), _amount0.capture(), _amount1.capture());

    assertEquals(BigInteger.valueOf(120), _amount0.getValue());
    assertEquals(BigInteger.valueOf(0), _amount1.getValue());
  }

  @Test
  void testAddLiquidityToLiquidityGross () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("2"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -240, 0, BigInteger.valueOf(100));

    assertEquals((Tick.Info.fromMap(pool.call("ticks", -240))).liquidityGross, BigInteger.valueOf(100));
    assertEquals((Tick.Info.fromMap(pool.call("ticks", 0))).liquidityGross, BigInteger.valueOf(100));
    assertEquals((Tick.Info.fromMap(pool.call("ticks", tickSpacing))).liquidityGross, ZERO);
    assertEquals((Tick.Info.fromMap(pool.call("ticks", tickSpacing * 2))).liquidityGross, ZERO);

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("3"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -240, tickSpacing, BigInteger.valueOf(150));
    
    assertEquals((Tick.Info.fromMap(pool.call("ticks", -240))).liquidityGross, BigInteger.valueOf(250));
    assertEquals((Tick.Info.fromMap(pool.call("ticks", 0))).liquidityGross, BigInteger.valueOf(100));
    assertEquals((Tick.Info.fromMap(pool.call("ticks", tickSpacing))).liquidityGross, BigInteger.valueOf(150));
    assertEquals((Tick.Info.fromMap(pool.call("ticks", tickSpacing * 2))).liquidityGross, ZERO);

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), 0, tickSpacing * 2, BigInteger.valueOf(60));
    
    assertEquals((Tick.Info.fromMap(pool.call("ticks", -240))).liquidityGross, BigInteger.valueOf(250));
    assertEquals((Tick.Info.fromMap(pool.call("ticks", 0))).liquidityGross, BigInteger.valueOf(160));
    assertEquals((Tick.Info.fromMap(pool.call("ticks", tickSpacing))).liquidityGross, BigInteger.valueOf(150));
    assertEquals((Tick.Info.fromMap(pool.call("ticks", tickSpacing * 2))).liquidityGross, BigInteger.valueOf(60));
  }

  @Test
  void testRemovesLiquidityFromLiquidityGross () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("2"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -240, 0, BigInteger.valueOf(100));
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -240, 0, BigInteger.valueOf(40));

    pool.invoke(alice, "burn", -240, 0, BigInteger.valueOf(90));
    
    assertEquals((Tick.Info.fromMap(pool.call("ticks", -240))).liquidityGross, BigInteger.valueOf(50));
    assertEquals((Tick.Info.fromMap(pool.call("ticks", 0))).liquidityGross, BigInteger.valueOf(50));
  }

  @Test
  void testClearsTickLowerIfLastPositionRemoved () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("2"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -240, 0, BigInteger.valueOf(100));

    pool.invoke(alice, "burn", -240, 0, BigInteger.valueOf(100));

    var tick = Tick.Info.fromMap(pool.call("ticks", -240));
    assertEquals(ZERO, tick.liquidityGross);
    assertEquals(ZERO, tick.feeGrowthOutside0X128);
    assertEquals(ZERO, tick.feeGrowthOutside1X128);
  }

  @Test
  void testClearsTickUpperIfLastPositionRemoved () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("2"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -240, 0, BigInteger.valueOf(100));

    pool.invoke(alice, "burn", -240, 0, BigInteger.valueOf(100));

    var tick = Tick.Info.fromMap(pool.call("ticks", 0));
    assertEquals(ZERO, tick.liquidityGross);
    assertEquals(ZERO, tick.feeGrowthOutside0X128);
    assertEquals(ZERO, tick.feeGrowthOutside1X128);
  }

  @Test
  void testClearsTickNotUsed () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("2"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -240, 0, BigInteger.valueOf(100));
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -tickSpacing, 0, BigInteger.valueOf(250));
    
    pool.invoke(alice, "burn", -240, 0, BigInteger.valueOf(100));

    var tick = Tick.Info.fromMap(pool.call("ticks", -240));
    assertEquals(ZERO, tick.liquidityGross);
    assertEquals(ZERO, tick.feeGrowthOutside0X128);
    assertEquals(ZERO, tick.feeGrowthOutside1X128);
    
    tick = Tick.Info.fromMap(pool.call("ticks", -tickSpacing));
    assertEquals(BigInteger.valueOf(250), tick.liquidityGross);
    assertEquals(ZERO, tick.feeGrowthOutside0X128);
    assertEquals(ZERO, tick.feeGrowthOutside1X128);
  }

  @Test
  void testDoesNotWriteAnObservation () {
    var start = TimeUtils.nowSeconds();
    Oracle.Observation observation = Oracle.Observation.fromMap(pool.call("observations", 0));
    Oracle.Observation expected = new Oracle.Observation(start, ZERO, ZERO, true);
    assertObservationEquals(expected, observation);

    sm.getBlock().increase(1);
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("2"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -240, 0, BigInteger.valueOf(100));
    
    observation = Oracle.Observation.fromMap(pool.call("observations", 0));
    expected = new Oracle.Observation(start, ZERO, ZERO, true);
    assertObservationEquals(expected, observation);
  }

  // Including current price
  @Test
  void testTransfersCurtrentPriceOfBothTokens () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("317"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("32"));

    reset(sicx.spy);
    reset(usdc.spy);

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, BigInteger.valueOf(100));
    
    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), BigInteger.valueOf(317), "pay".getBytes());
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), BigInteger.valueOf(32), "pay".getBytes());

    assertEquals(sicx.score.call("balanceOf", pool.getAddress()), BigInteger.valueOf(9996 + 317));
    assertEquals(usdc.score.call("balanceOf", pool.getAddress()), BigInteger.valueOf(1000 + 32));
  }
  
  @Test
  void testInitializesLowerTick () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("317"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("32"));

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, BigInteger.valueOf(100));
    
    var tick = Tick.Info.fromMap(pool.call("ticks", minTick + tickSpacing));
    assertEquals(BigInteger.valueOf(100), tick.liquidityGross);
  }
  
  @Test
  void testInitializesUpperTick () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("317"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("32"));

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, BigInteger.valueOf(100));
    
    var tick = Tick.Info.fromMap(pool.call("ticks", maxTick - tickSpacing));
    assertEquals(BigInteger.valueOf(100), tick.liquidityGross);
  }

  @Test
  void testWorksForMinMaxTick () {
    

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("31623"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("3163"));

    reset(sicx.spy);
    reset(usdc.spy);

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick, maxTick, BigInteger.valueOf(10000));
    
    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), BigInteger.valueOf(31623), "pay".getBytes());
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), BigInteger.valueOf(3163), "pay".getBytes());

    assertEquals(sicx.score.call("balanceOf", pool.getAddress()), BigInteger.valueOf(9996 + 31623));
    assertEquals(usdc.score.call("balanceOf", pool.getAddress()), BigInteger.valueOf(1000 + 3163));
  }

  @Test
  void testRemovingWorksIncludingCurrentPrice () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("317"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("32"));

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, BigInteger.valueOf(100));
    
    pool.invoke(alice, "burn", minTick + tickSpacing, maxTick - tickSpacing, BigInteger.valueOf(100));

    reset(pool.spy);
    pool.invoke(alice, "collect", alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, IntUtils.MAX_UINT256, IntUtils.MAX_UINT256);

    // Get Collect event
    ArgumentCaptor<Address> _caller = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _tickLower = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickUpper = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(pool.spy).Collect(_caller.capture(), _tickLower.capture(), _tickUpper.capture(), _recipient.capture(), _amount0.capture(), _amount1.capture());

    assertEquals(BigInteger.valueOf(316), _amount0.getValue());
    assertEquals(BigInteger.valueOf(31),  _amount1.getValue());
  }

  @Test
  void testWritesAnObservation () {
    var start = TimeUtils.nowSeconds();
    Oracle.Observation observation = Oracle.Observation.fromMap(pool.call("observations", 0));
    Oracle.Observation expected = new Oracle.Observation(start, ZERO, ZERO, true);
    assertObservationEquals(expected, observation);

    sm.getBlock().increase(1);
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("317"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("32"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick, maxTick, BigInteger.valueOf(100));
    
    observation = Oracle.Observation.fromMap(pool.call("observations", 0));
    expected = new Oracle.Observation(start.add(TWO), BigInteger.valueOf(-23028), new BigInteger("107650226801941937191829992860413859"), true);
    assertObservationEquals(expected, observation);
  }

  @Test
  void testTransfersToken1Only () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("2162"));
    
    reset(sicx.spy);
    reset(usdc.spy);

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -46080, -23040, BigInteger.valueOf(10000));

    verifyNoInteractions(sicx.spy);
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), BigInteger.valueOf(2162), "pay".getBytes());

    assertEquals(BigInteger.valueOf(9996), sicx.call("balanceOf", pool.getAddress()));
    assertEquals(BigInteger.valueOf(1000 + 2162), usdc.call("balanceOf", pool.getAddress()));
  }

  @Test
  void testMinTickWithMaxLeverage () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("828011520"));
    
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick, minTick + tickSpacing, TWO.pow(102));

    assertEquals(BigInteger.valueOf(9996), sicx.call("balanceOf", pool.getAddress()));
    assertEquals(BigInteger.valueOf(1000 + 828011520), usdc.call("balanceOf", pool.getAddress()));
  }

  @Test
  void testWorksForMinTickBelowCurrentPrice () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("3161"));
    
    reset(sicx.spy);
    reset(usdc.spy);

    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick, -23040, BigInteger.valueOf(10000));

    verifyNoInteractions(sicx.spy);
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), BigInteger.valueOf(3161), "pay".getBytes());

    assertEquals(BigInteger.valueOf(9996), sicx.call("balanceOf", pool.getAddress()));
    assertEquals(BigInteger.valueOf(1000 + 3161), usdc.call("balanceOf", pool.getAddress()));
  }

  @Test
  void testRemovingWorksBelowCurrentPrice () {

    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("4"));
    
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -46080, -46020, BigInteger.valueOf(10000));
    pool.invoke(alice, "burn", -46080, -46020, BigInteger.valueOf(10000));
    
    reset(pool.spy);
    pool.invoke(alice, "collect", alice.getAddress(), -46080, -46020, IntUtils.MAX_UINT256, IntUtils.MAX_UINT256);
    
    // Get Collect event
    ArgumentCaptor<Address> _caller = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _tickLower = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickUpper = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(pool.spy).Collect(_caller.capture(), _tickLower.capture(), _tickUpper.capture(), _recipient.capture(), _amount0.capture(), _amount1.capture());

    assertEquals(BigInteger.valueOf(0), _amount0.getValue());
    assertEquals(BigInteger.valueOf(3), _amount1.getValue());
  }

  @Test
  void testDoesNotWriteAnObservationBelowCurrentPrice () {
    var start = TimeUtils.nowSeconds();
    Oracle.Observation observation = Oracle.Observation.fromMap(pool.call("observations", 0));
    Oracle.Observation expected = new Oracle.Observation(start, ZERO, ZERO, true);
    assertObservationEquals(expected, observation);

    sm.getBlock().increase(1);
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("22"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -46080, -23040, BigInteger.valueOf(100));
    
    observation = Oracle.Observation.fromMap(pool.call("observations", 0));
    expected = new Oracle.Observation(start, ZERO, ZERO, true);
    assertObservationEquals(expected, observation);
  }

  @Test
  void testProtocolAccumulateDuringSwap () {
    pool.invoke(owner, "setFeeProtocol", 6, 6);

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("3162277660168379332"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("316227766016837934"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, TEN.pow(18));

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("100000000000000000"));
    swapExact0For1(TEN.pow(18).divide(TEN), alice);
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("10000000000000000"));
    swapExact1For0(TEN.pow(18).divide(HUNDRED), alice);

    var fees = ProtocolFees.fromMap(pool.call("protocolFees"));
    assertEquals(new BigInteger("50000000000000"), fees.token0);
    assertEquals(new BigInteger("5000000000000"), fees.token1);
  }

  @Test
  void testPositionsAreProtectedBeforeProtocolFeeTurnedOn () {
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("3162277660168379332"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("316227766016837934"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, TEN.pow(18));
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("100000000000000000"));
    swapExact0For1(TEN.pow(18).divide(TEN), alice);
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("10000000000000000"));
    swapExact1For0(TEN.pow(18).divide(HUNDRED), alice);
    
    var fees = ProtocolFees.fromMap(pool.call("protocolFees"));
    assertEquals(ZERO, fees.token0);
    assertEquals(ZERO, fees.token1);

    pool.invoke(owner, "setFeeProtocol", 6, 6);
    fees = ProtocolFees.fromMap(pool.call("protocolFees"));
    assertEquals(ZERO, fees.token0);
    assertEquals(ZERO, fees.token1);
  }

  @Test
  void testPokeNotAllowedOnUnitializedPosition () {

    ConvexusLiquidity.deposit(bob, callee.getAddress(), sicx.score, new BigInteger("3162277660168379332"));
    ConvexusLiquidity.deposit(bob, callee.getAddress(), usdc.score, new BigInteger("316227766016837934"));
    callee.invoke(bob, "mint", pool.getAddress(), bob.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, TEN.pow(18));
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("100000000000000000"));
    swapExact0For1(TEN.pow(18).divide(TEN), alice);
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("10000000000000000"));
    swapExact1For0(TEN.pow(18).divide(HUNDRED), alice);

    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      pool.invoke(alice, "burn", minTick + tickSpacing, maxTick - tickSpacing, ZERO),
      "update: pokes aren't allowed for 0 liquidity positions");
  }
  
  @Test
  void testPokeNotAllowedOnUnitializedPosition2 () {

    ConvexusLiquidity.deposit(bob, callee.getAddress(), sicx.score, new BigInteger("3162277660168379332"));
    ConvexusLiquidity.deposit(bob, callee.getAddress(), usdc.score, new BigInteger("316227766016837934"));
    callee.invoke(bob, "mint", pool.getAddress(), bob.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, TEN.pow(18));
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("100000000000000000"));
    swapExact0For1(TEN.pow(18).divide(TEN), alice);
    
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("10000000000000000"));
    swapExact1For0(TEN.pow(18).divide(HUNDRED), alice);

    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("4"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing, ONE);

    var position = Position.Info.fromMap(pool.call("positions", Positions.getKey(alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing)));
    assertEquals(ONE, position.liquidity);
    assertEquals(new BigInteger("102084710076281216349243831104605583"), position.feeGrowthInside0LastX128);
    assertEquals(new BigInteger("10208471007628121634924383110460558"), position.feeGrowthInside1LastX128);
    assertEquals(ZERO, position.tokensOwed0);
    assertEquals(ZERO, position.tokensOwed1);
    
    pool.invoke(alice, "burn", minTick + tickSpacing, maxTick - tickSpacing, ONE);
    position = Position.Info.fromMap(pool.call("positions", Positions.getKey(alice.getAddress(), minTick + tickSpacing, maxTick - tickSpacing)));
    assertEquals(ZERO, position.liquidity);
    assertEquals(new BigInteger("102084710076281216349243831104605583"), position.feeGrowthInside0LastX128);
    assertEquals(new BigInteger("10208471007628121634924383110460558"), position.feeGrowthInside1LastX128);
    assertEquals(BigInteger.valueOf(3), position.tokensOwed0);
    assertEquals(ZERO, position.tokensOwed1);
  }
}