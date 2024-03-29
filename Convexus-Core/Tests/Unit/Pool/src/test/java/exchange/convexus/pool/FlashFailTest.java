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

import static java.math.BigInteger.ONE;
import static org.mockito.Mockito.reset;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.test.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.AssertUtils;

public class FlashFailTest extends ConvexusPoolTest {

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
  }

  @Test
  void testFailsIfNotInitialized () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      flash(alice, "100", "200", bob, "0", "0"), 
      "unlock: pool isn't initialized yet");

    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      flash(alice, "100", "0", bob, "0", "0"), 
      "unlock: pool isn't initialized yet");

    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      flash(alice, "0", "200", bob, "0", "0"), 
      "unlock: pool isn't initialized yet");
  }

  @Test
  void testFailsIfNoLiquidity1 () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      flash(alice, "100", "200", bob, "0", "0"), 
      "flash: no liquidity");
  }

  @Test
  void testFailsIfNoLiquidity2 () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      flash(alice, "100", "0", bob, "0", "0"), 
      "flash: no liquidity");
  }

  @Test
  void testFailsIfNoLiquidity3 () {
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      flash(alice, "0", "200", bob, "0", "0"), 
      "flash: no liquidity");
  }
}
