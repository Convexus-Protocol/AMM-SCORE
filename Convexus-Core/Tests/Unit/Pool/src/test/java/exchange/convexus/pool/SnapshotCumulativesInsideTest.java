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

package exchange.convexus.pool;

import static exchange.convexus.utils.SleepUtils.sleep;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
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
import exchange.convexus.utils.AssertUtils;

public class SnapshotCumulativesInsideTest extends ConvexusPoolTest {

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
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    mint(alice, tickLower, tickUpper, BigInteger.valueOf(10), "1", "1");
  }

  @Test
  void testThrowsIfTicksAreInReverseOrder () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.call("snapshotCumulativesInside", tickUpper, tickLower),
      "checkTicks: tickLower must be lower than tickUpper");
  }
  
  @Test
  void testThrowsIfTicksAreTheSame () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.call("snapshotCumulativesInside", tickUpper, tickUpper),
      "checkTicks: tickLower must be lower than tickUpper");
  }
  
  @Test
  void testThrowsIfTicksLowerIsTooLow () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.call("snapshotCumulativesInside", getMinTick(tickSpacing) - 1, tickUpper),
      "snapshotCumulativesInside: lower not initialized");
  }

  @Test
  void testThrowsIfTicksUpperIsTooHigh () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.call("snapshotCumulativesInside", tickLower, (getMaxTick(tickSpacing) + 1)),
      "snapshotCumulativesInside: upper not initialized");
  }

  @Test
  void testThrowsIfTicksLowerIsNotInitialized () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.call("snapshotCumulativesInside", tickLower - tickSpacing, tickUpper),
      "snapshotCumulativesInside: lower not initialized");
  }

  @Test
  void testThrowsIfTicksUpperIsNotInitialized () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> pool.call("snapshotCumulativesInside", tickLower, tickUpper + tickSpacing),
      "snapshotCumulativesInside: upper not initialized");
  }

  @Test
  void testIsZeroImmediatelyAfterInitialize () {
    var result = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", tickLower, tickUpper));
    assertEquals(ZERO, result.secondsPerLiquidityInsideX128);
    assertEquals(ZERO, result.tickCumulativeInside);
    assertEquals(ZERO, result.secondsInside);
  }

  @Test
  void testIncreasesByExpectedAmountWhenTimeElapsesInTheRage () {
    sleep(5);
    var result = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", tickLower, tickUpper));
    assertEquals(BigInteger.valueOf(5).shiftLeft(128).divide(TEN), result.secondsPerLiquidityInsideX128);
    assertEquals(ZERO, result.tickCumulativeInside);
    assertEquals(BigInteger.valueOf(5), result.secondsInside);
  }

  @Test
  void testDoesNotAccountForTimeIncreaseAboveRange () {
    sleep(5);
    swapToHigherPrice(alice, encodePriceSqrt(TWO, ONE), "2");
    sleep(7);
    
    var result = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", tickLower, tickUpper));
    assertEquals(BigInteger.valueOf(5).shiftLeft(128).divide(TEN), result.secondsPerLiquidityInsideX128);
    assertEquals(ZERO, result.tickCumulativeInside);
    assertEquals(BigInteger.valueOf(5), result.secondsInside);
  }
  
  @Test
  void testDoesNotAccountForTimeIncreaseBelowRange () {
    sleep(5);
    swapToLowerPrice(alice, encodePriceSqrt(ONE, TWO), "2");
    sleep(7);
    
    var result = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", tickLower, tickUpper));
    assertEquals(BigInteger.valueOf(5).shiftLeft(128).divide(TEN), result.secondsPerLiquidityInsideX128);
    // tick is 0 for 5 seconds, then not in range
    assertEquals(ZERO, result.tickCumulativeInside);
    assertEquals(BigInteger.valueOf(5), result.secondsInside);
  }

  @Test
  void testTimeIncreaseBelowRangeIsNotCounted () {
    swapToLowerPrice(alice, encodePriceSqrt(ONE, TWO), "2");
    sleep(5);
    swapToHigherPrice(alice, encodePriceSqrt(ONE, ONE), "2");
    sleep(7);
    
    var result = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", tickLower, tickUpper));
    assertEquals(BigInteger.valueOf(7).shiftLeft(128).divide(TEN), result.secondsPerLiquidityInsideX128);
    // tick is not in range then tick is 0 for 7 seconds
    assertEquals(ZERO, result.tickCumulativeInside);
    assertEquals(BigInteger.valueOf(7), result.secondsInside);
  }

  @Test
  void testTimeIncreaseAboveRangeIsNotCounted () {
    swapToHigherPrice(alice, encodePriceSqrt(TWO, ONE), "2");
    sleep(5);
    swapToLowerPrice(alice, encodePriceSqrt(ONE, ONE), "2");
    sleep(7);
    
    var result = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", tickLower, tickUpper));
    assertEquals(BigInteger.valueOf(7).shiftLeft(128).divide(TEN), result.secondsPerLiquidityInsideX128);
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    // justify the -7 tick cumulative inside value
    assertEquals(-1, slot0.tick);
    assertEquals(BigInteger.valueOf(-7), result.tickCumulativeInside);
    assertEquals(BigInteger.valueOf(7), result.secondsInside);
  }

  @Test
  void testPositionsMintedAfterTimeSpent () {
    sleep(5);
    mint(alice, tickUpper, getMaxTick(tickSpacing), BigInteger.valueOf(15), "15", "0");
    swapToHigherPrice(alice, encodePriceSqrt(TWO, ONE), "10");
    sleep(8);

    var result = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", tickUpper, getMaxTick(tickSpacing)));
    assertEquals(BigInteger.valueOf(8).shiftLeft(128).divide(BigInteger.valueOf(15)), result.secondsPerLiquidityInsideX128);
    // the tick of 2/1 is 6931
    // 8 seconds * 6931 = 55448
    assertEquals(BigInteger.valueOf(55448), result.tickCumulativeInside);
    assertEquals(BigInteger.valueOf(8), result.secondsInside);
  }

  @Test
  void overlappingLiquidityIsAggregated () {
    mint(alice, tickLower, getMaxTick(tickSpacing), BigInteger.valueOf(15), "15", "1");
    sleep(5);
    swapToHigherPrice(alice, encodePriceSqrt(TWO, ONE), "10");
    sleep(8);
    
    var result = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", tickLower, tickUpper));
    assertEquals(BigInteger.valueOf(5).shiftLeft(128).divide(BigInteger.valueOf(25)), result.secondsPerLiquidityInsideX128);
    assertEquals(BigInteger.valueOf(0), result.tickCumulativeInside);
    assertEquals(BigInteger.valueOf(5), result.secondsInside);
  }

  @Test
  void testRelativeBehaviorOfSnapshots () {
    sleep(5);
    mint(alice, getMinTick(tickSpacing), tickLower, BigInteger.valueOf(15), "0", "15");

    var start = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", getMinTick(tickSpacing), tickLower));
    sleep(8);
    
    // 13 seconds in starting range, then 3 seconds in newly minted range
    swapToLowerPrice(alice, encodePriceSqrt(ONE, TWO), "10");
    sleep(3);
    
    var result = SnapshotCumulativesInsideResult.fromMap(pool.call("snapshotCumulativesInside", getMinTick(tickSpacing), tickLower));

    BigInteger expectedDiffSecondsPerLiquidity = BigInteger.valueOf(3).shiftLeft(128).divide(BigInteger.valueOf(15));
    assertEquals(expectedDiffSecondsPerLiquidity, result.secondsPerLiquidityInsideX128.subtract(start.secondsPerLiquidityInsideX128));
    // the tick is the one corresponding to the price of 1/2, or log base 1.0001 of 0.5
    // this is -6932, and 3 seconds have passed, so the cumulative computed from the diff equals 6932 * 3
    assertEquals(BigInteger.valueOf(-20796), result.tickCumulativeInside.subtract(start.tickCumulativeInside));
    assertEquals(BigInteger.valueOf(3), result.secondsInside.subtract(start.secondsInside));
    assertNotEquals(BigInteger.valueOf(3), result.secondsInside);
  }
}