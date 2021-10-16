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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.AssertUtils;

import static java.math.BigInteger.ONE;

public class IncreaseObservationCardinalityNextTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int TICK_SPACING = TICK_SPACINGS[MEDIUM];

  @BeforeEach
  void setup() throws Exception {
    setup_factory();
    reset(factory.spy);
    setup_pool(factory.getAddress(), FEE, TICK_SPACING);
    reset(pool.spy);

    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE, pool.getAddress());
  }

  @Test
  void testOnlyAfterInitialize() {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      pool.invoke(alice, "increaseObservationCardinalityNext", 2), 
      "ReentrancyLock: wrong lock state: true");
  }
  
  @Test
  void testEmitsEventBothOldAndNew() {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    pool.invoke(alice, "increaseObservationCardinalityNext", 2);
    // Get IncreaseObservationCardinalityNext event
    ArgumentCaptor<Integer> _old = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _new = ArgumentCaptor.forClass(Integer.class);
    verify(pool.spy).IncreaseObservationCardinalityNext(_old.capture(), _new.capture());
    assertEquals(_old.getValue(), 1);
    assertEquals(_new.getValue(), 2);
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
    pool.invoke(alice, "increaseObservationCardinalityNext", 3);
    pool.invoke(alice, "increaseObservationCardinalityNext", 2);
    Slot0 slot0 = (Slot0) pool.call("slot0");
    assertEquals(3, slot0.observationCardinalityNext);
  }

  @Test
  void testIncreasesCardinalityAndNextFirstTime () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    pool.invoke(alice, "increaseObservationCardinalityNext", 2);
    Slot0 slot0 = (Slot0) pool.call("slot0");
    assertEquals(1, slot0.observationCardinality);
    assertEquals(2, slot0.observationCardinalityNext);
  }
}
