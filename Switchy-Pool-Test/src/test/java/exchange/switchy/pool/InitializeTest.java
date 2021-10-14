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

package exchange.switchy.pool;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.switchy.factory.SwitchyFactoryUtils;


public class InitializeTest extends SwitchyPoolTest {
  
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

    SwitchyFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE, pool.getAddress());
  }

  @Test
  void testAssumptions() {
    assertEquals(factory.getAddress(), pool.call("factory"));
    assertEquals(sicx.getAddress(), pool.call("token0"));
    assertEquals(usdc.getAddress(), pool.call("token1"));
    assertEquals(getMaxLiquidityPerTick(TICK_SPACING), pool.call("maxLiquidityPerTick"));
  }

  private BigInteger getMaxLiquidityPerTick(int tickSpacing) {
    return BigInteger.TWO.pow(128).subtract(ONE).divide(
      BigInteger.valueOf((getMaxTick(tickSpacing) - getMinTick(tickSpacing)) / tickSpacing + 1)
    );
  }

  private int getMinTick(int tickSpacing) {
    return ((int) Math.ceil(-887272 / tickSpacing)) * tickSpacing;
  }

  private int getMaxTick(int tickSpacing) {
    return ((int) Math.floor(887272 / tickSpacing)) * tickSpacing;
  }
}
