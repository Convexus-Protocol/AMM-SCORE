/*
 * Copyright 2022 ICONation
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
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;

public class CollectTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[LOW];
  int tickSpacing = TICK_SPACINGS[LOW];

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
  }

  @Test
  void testWorksWithMultipleLPs () {
    mint(alice, minTick, maxTick, expandTo18Decimals(1), "1000000000000000000", "1000000000000000000");
    mint(alice, minTick + tickSpacing, maxTick - tickSpacing, expandTo18Decimals(2), "2000000000000000000", "2000000000000000000");

    swapExact0For1(expandTo18Decimals(1), alice);
    
    // poke positions
    burn(minTick, maxTick, ZERO);
    burn(minTick + tickSpacing, maxTick - tickSpacing, ZERO);

    var position = positions(alice, minTick, maxTick);
    BigInteger tokensOwed0Position0 = position.tokensOwed0;
    
    position = positions(alice, minTick + tickSpacing, maxTick - tickSpacing);
    BigInteger tokensOwed0Position1 = position.tokensOwed0;

    assertEquals(tokensOwed0Position0, new BigInteger("166666666666667"));
    assertEquals(tokensOwed0Position1, new BigInteger("333333333333334"));
  }
}
