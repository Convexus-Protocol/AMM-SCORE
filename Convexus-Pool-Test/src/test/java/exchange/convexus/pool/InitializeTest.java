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

import static exchange.convexus.librairies.TickMath.MAX_SQRT_RATIO;
import static exchange.convexus.librairies.TickMath.MIN_SQRT_RATIO;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.librairies.Oracle;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.TimeUtils;

public class InitializeTest extends ConvexusPoolTest {
  
  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];

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
  void testAssumptions() {
    assertEquals(factory.getAddress(), pool.call("factory"));
    assertEquals(sicx.getAddress(), pool.call("token0"));
    assertEquals(usdc.getAddress(), pool.call("token1"));
    assertEquals(getMaxLiquidityPerTick(tickSpacing), pool.call("maxLiquidityPerTick"));
  }

  @Test
  void testAlreadyInitialized () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE)),
      "initialize: this pool is already initialized");
  }

  @Test
  void testInitializeFromSomeoneElse () {
    pool.invoke(bob, "initialize", encodePriceSqrt(ONE, ONE));
  }

  @Test
  void testStartPriceTooLow () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      pool.invoke(alice, "initialize", ONE),
      "getTickAtSqrtRatio: preconditions failed");

    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      pool.invoke(alice, "initialize", MIN_SQRT_RATIO.subtract(ONE)),
      "getTickAtSqrtRatio: preconditions failed");
  }

  @Test
  void testStartPriceTooHigh () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      pool.invoke(alice, "initialize", MAX_SQRT_RATIO),
      "getTickAtSqrtRatio: preconditions failed");
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      pool.invoke(alice, "initialize", TWO.pow(160)),
      "getTickAtSqrtRatio: preconditions failed");
  }

  @Test
  void testCanInitializeMinRatio () {
    pool.invoke(alice, "initialize", MIN_SQRT_RATIO);
    assertEquals(((Slot0) pool.call("slot0")).tick, getMinTick(1));
  }

  @Test
  void testCanInitializeMaxRatio () {
    pool.invoke(alice, "initialize", MAX_SQRT_RATIO.subtract(ONE));
    assertEquals(((Slot0) pool.call("slot0")).tick, getMaxTick(1) - 1);
  }

  @Test
  void testInitialVariables () {
    final BigInteger price = encodePriceSqrt(ONE, TWO);
    pool.invoke(alice, "initialize", price);
    Slot0 slot0 = (Slot0) pool.call("slot0");
    assertEquals(price, slot0.sqrtPriceX96);
    assertEquals(0, slot0.observationIndex);
    assertEquals(-6932, slot0.tick);
  }

  @Test
  void testFirstObservationsSlot () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));

    Oracle.Observation observation = (Oracle.Observation) pool.call("observations", 0);
    Oracle.Observation expected = new Oracle.Observation(TimeUtils.nowSeconds(), ZERO, ZERO, true);
    assertObservationEquals(expected, observation);
  }

  @Test
  void testInitializedEventWithInputTick () {
    final BigInteger sqrtPriceX96 = encodePriceSqrt(ONE, TWO);
    pool.invoke(alice, "initialize", sqrtPriceX96);
    // Get Initialize event
    ArgumentCaptor<BigInteger> _sqrtPriceX96 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<Integer> _tick = ArgumentCaptor.forClass(Integer.class);
    verify(pool.spy).Initialize(_sqrtPriceX96.capture(), _tick.capture());
    assertEquals(_sqrtPriceX96.getValue(), sqrtPriceX96);
    assertEquals(_tick.getValue(), -6932);
  }
}
