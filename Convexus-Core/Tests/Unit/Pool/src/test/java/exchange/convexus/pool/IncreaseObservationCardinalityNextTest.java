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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import exchange.convexus.test.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.AssertUtils;
import static exchange.convexus.utils.TimeUtils.now;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public class IncreaseObservationCardinalityNextTest extends ConvexusPoolTest {

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
  void testOnlyAfterInitialize() {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      pool.invoke(alice, "increaseObservationCardinalityNext", 2), 
      "unlock: pool isn't initialized yet");
  }
  
  @Test
  void testEmitsEventBothOldAndNew() {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    reset(pool.spy);
    pool.invoke(alice, "increaseObservationCardinalityNext", 2048);
    // Get IncreaseObservationCardinalityNext event
    ArgumentCaptor<Integer> _old = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _new = ArgumentCaptor.forClass(Integer.class);
    verify(pool.spy).IncreaseObservationCardinalityNext(_old.capture(), _new.capture());
    assertEquals(_old.getValue(), 1024);
    assertEquals(_new.getValue(), 2048);
  }

  @Test
  void testNoEmitForNoop () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    pool.invoke(alice, "increaseObservationCardinalityNext", 3);
    reset(pool.spy);
    pool.invoke(alice, "increaseObservationCardinalityNext", 2);
    // Check if IncreaseObservationCardinalityNext event is not emitted
    ArgumentCaptor<Integer> _old = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _new = ArgumentCaptor.forClass(Integer.class);
    verify(pool.spy, never()).IncreaseObservationCardinalityNext(_old.capture(), _new.capture());
  }

  @Test
  void testNotChangeIfLessThanCurrent () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    pool.invoke(alice, "increaseObservationCardinalityNext", 2048);
    pool.invoke(alice, "increaseObservationCardinalityNext", 2047);
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(2048, slot0.observationCardinalityNext);
  }

  @Test
  void testIncreasesCardinalityAndNextFirstTime () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    pool.invoke(alice, "increaseObservationCardinalityNext", 2048);
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(1, slot0.observationCardinality);
    assertEquals(2048, slot0.observationCardinalityNext);
  }

  @Test
  void testOracleStartingStateAfterInitialization () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    var slot0 = Slot0.fromMap(pool.call("slot0"));

    assertEquals(1, slot0.observationCardinality);
    assertEquals(0, slot0.observationIndex);
    assertEquals(1024, slot0.observationCardinalityNext);

    var observation = Oracle.Observation.fromMap(pool.call("observations", 0));

    assertEquals(ZERO, observation.secondsPerLiquidityCumulativeX128);
    assertEquals(ZERO, observation.tickCumulative);
    assertEquals(true, observation.initialized);
    assertEquals(now(), observation.blockTimestamp);
  }

  @Test
  void testIncreasesObservationCardinalityNext () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    pool.invoke(alice, "increaseObservationCardinalityNext", 2);

    var slot0 = Slot0.fromMap(pool.call("slot0"));

    assertEquals(1, slot0.observationCardinality);
    assertEquals(0, slot0.observationIndex);
    assertEquals(1024, slot0.observationCardinalityNext);
  }

  @Test
  void testNoOpIfTargetIsAlreadyExceeded () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    pool.invoke(alice, "increaseObservationCardinalityNext", 2048);
    pool.invoke(alice, "increaseObservationCardinalityNext", 3);
    
    var slot0 = Slot0.fromMap(pool.call("slot0"));

    assertEquals(1, slot0.observationCardinality);
    assertEquals(0, slot0.observationIndex);
    assertEquals(2048, slot0.observationCardinalityNext);
  }
}
