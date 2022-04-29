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
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.test.factory.ConvexusFactoryUtils;

public class ObserveTest extends ConvexusPoolTest {

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

    // initialize at zero tick
    initializeAtZeroTick();
  }

  @Test
  void testCurrentTickAccumulatorIncreasesByTickOverTime () {
    BigInteger[] secondsAgos = { ZERO };

    var observation = ObserveResult.fromMap(pool.call("observe", new Object[] {secondsAgos}));
    assertEquals(ZERO, observation.tickCumulatives[0]);
    
    sleep(10);
    
    observation = ObserveResult.fromMap(pool.call("observe", new Object[] {secondsAgos}));
    assertEquals(ZERO, observation.tickCumulatives[0]);
  }

  @Test
  void testCurrentTickAccumulatorAfterSingleSwap () {
    // moves to tick -1
    swapExact0For1(BigInteger.valueOf(1000), alice);
    sleep(4);
    
    BigInteger[] secondsAgos = { ZERO };
    var observation = ObserveResult.fromMap(pool.call("observe", new Object[] {secondsAgos}));
    assertEquals(BigInteger.valueOf(-4), observation.tickCumulatives[0]);
  }

  @Test
  void testCurrentTickAccumulatorAfterTwoSwaps () {
    swapExact0For1(TEN.pow(18).divide(TWO), alice);
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(-4452, slot0.tick);
    
    sleep(4);

    swapExact1For0(TEN.pow(18).divide(BigInteger.valueOf(4)), alice);
    slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(-1558, slot0.tick);

    sleep(6);
    
    BigInteger[] secondsAgos = { ZERO };
    var observation = ObserveResult.fromMap(pool.call("observe", new Object[] {secondsAgos}));
    // -27156 = -4452*4 + -1558*6
    assertEquals(BigInteger.valueOf(-27156), observation.tickCumulatives[0]);
  }
}