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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.test.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.AssertUtils;

public class TickSpacingTest extends ConvexusPoolTest {

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
    setup_pool(factory.getAddress(), FEE_AMOUNTS[MEDIUM], 12);
    reset(pool.spy);

    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE, pool.getAddress());
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
  }

  @Test
  void testMintCanOnlyBeCalledForMultiplesOf12_1 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> mint(alice, -6, 0, ONE, "1", "1"),
      "flipTick: tick isn't spaced");
  }

  @Test
  void testMintCanOnlyBeCalledForMultiplesOf12_2 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> mint(alice, 0, 6, ONE, "1", "1"),
      "flipTick: tick isn't spaced");
  }

  @Test
  void testMintCanBeCalledWithMultipleOf12 () {
    mint(alice, 12, 24, ONE, "1", "0");
    mint(alice, -144, -120, ONE, "0", "1");
  }

  @Test
  void testSwappingAcrossGapsWorksIn1For0Direction () {
    var liquidityAmount = expandTo18Decimals(1).divide(BigInteger.valueOf(4));
    mint(alice, 120000, 121200, liquidityAmount, "36096898321357", "0");

    swapExact1For0(expandTo18Decimals(1), alice);

    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    burn(120000, 121200, liquidityAmount);
    verify(pool.spy).Burn(alice.getAddress(), 120000, 121200, liquidityAmount, new BigInteger("30027458295511"), new BigInteger("996999999999999999"));
    verifyNoInteractions(sicx.spy);
    verifyNoInteractions(usdc.spy);

    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(slot0.tick, 120196);
  }

  @Test
  void testSwappingAcrossGapsWorksIn0For1Direction () {
    var liquidityAmount = expandTo18Decimals(1).divide(BigInteger.valueOf(4));
    mint(alice, -121200, -120000, liquidityAmount, "0", "36096898321357");

    swapExact0For1(expandTo18Decimals(1), alice);

    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    burn(-121200, -120000, liquidityAmount);
    verify(pool.spy).Burn(alice.getAddress(), -121200, -120000, liquidityAmount, new BigInteger("996999999999999999"), new BigInteger("30027458295511"));
    verifyNoInteractions(sicx.spy);
    verifyNoInteractions(usdc.spy);

    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(slot0.tick, -120197);
  }
}
