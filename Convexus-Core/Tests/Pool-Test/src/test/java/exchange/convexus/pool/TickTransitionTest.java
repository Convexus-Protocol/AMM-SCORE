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

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.librairies.TickMath;

public class TickTransitionTest extends ConvexusPoolTest {

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
    setup_pool(factory.getAddress(), FEE, 1);
    reset(pool.spy);
  }

  @Test
  void testTickTransitionCannotRunTwiceIfZeroForOneSwapEndsAtFractionalPriceJustBelowTick () {
    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE);

    // initialize at a price of ~0.3 token1/token0
    // meaning if you swap in 2 token0, you should end up getting 0 token1
    var p0 = TickMath.getSqrtRatioAtTick(-24081).add(ONE);
    pool.invoke(alice, "initialize", p0);

    assertEquals(ZERO, pool.call("liquidity"));

    var slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(slot0.tick, -24081);

    // add a bunch of liquidity around current price
    var liquidity = expandTo18Decimals(1000);
    mint(alice, -24082, -24080, liquidity, "166657000384412591", "14998620157219703");
    assertEquals(liquidity, pool.call("liquidity"));

    mint(alice, -24082, -24081, liquidity, "0", "14998620157219703");
    assertEquals(liquidity, pool.call("liquidity"));

    // check the math works out to moving the price down 1, sending no amount out, and having some amount remaining
    BigInteger THREE = BigInteger.valueOf(3);
    ComputeSwapStepResult result = SwapMath.computeSwapStep(p0, p0.subtract(ONE), liquidity, THREE, FEE_AMOUNTS[MEDIUM]);
    assertEquals(p0.subtract(ONE), result.sqrtRatioNextX96);
    assertEquals(ONE, result.feeAmount);
    assertEquals(ONE, result.amountIn);
    assertEquals(ZERO, result.amountOut);

    // swap 2 amount in, should get 0 amount out
    reset(sicx.spy);
    reset(usdc.spy);
    swapExact0For1(THREE, alice);
    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), THREE, "{\"method\": \"pay\"}".getBytes());
    verifyNoInteractions(usdc.spy);

    slot0 = Slot0.fromMap(pool.call("slot0"));

    assertEquals(-24082, slot0.tick);
    assertEquals(p0.subtract(ONE), result.sqrtRatioNextX96);
    assertEquals(liquidity.multiply(TWO), pool.call("liquidity"));
  }
}
