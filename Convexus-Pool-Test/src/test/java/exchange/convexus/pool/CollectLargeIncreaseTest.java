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
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.IntUtils;

class CollectLargeIncreaseTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[LOW];
  int tickSpacing = TICK_SPACINGS[LOW];

  int minTick = getMinTick(tickSpacing);
  int maxTick = getMaxTick(tickSpacing);

  // UINT128_MAX * 2**128 / 1e18
  // https://www.wolframalpha.com/input/?i=%282**128+-+1%29+*+2**128+%2F+1e18
  BigInteger magicNumber = new BigInteger("115792089237316195423570985008687907852929702298719625575994");

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_factory();
    reset(factory.spy);
    setup_pool(factory.getAddress(), FEE, tickSpacing);
    reset(pool.spy);

    // Transfer some funds to Alice
    sicx.invoke(owner, "mintTo", alice.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));
    usdc.invoke(owner, "mintTo", alice.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));
    // Transfer some funds to Bob
    sicx.invoke(owner, "mintTo", bob.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));
    usdc.invoke(owner, "mintTo", bob.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));

    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE, pool.getAddress());
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    mint(alice, minTick, maxTick, expandTo18Decimals(1), "1000000000000000000", "1000000000000000000");
  }

  @Test
  void testWorksJustBeforeTheCapBinds () {
    setFeeGrowthGlobal0X128(magicNumber);
    burn(minTick, maxTick, ZERO);

    var position = positions(alice, minTick, maxTick);

    assertEquals(IntUtils.MAX_UINT128.subtract(ONE), position.tokensOwed0);
    assertEquals(ZERO, position.tokensOwed1);
  }

  @Test
  void testWorksJustAfterCapBinds () {
    setFeeGrowthGlobal0X128(magicNumber.add(ONE));
    burn(minTick, maxTick, ZERO);

    var position = positions(alice, minTick, maxTick);

    assertEquals(IntUtils.MAX_UINT128, position.tokensOwed0);
    assertEquals(ZERO, position.tokensOwed1);
  }

  @Test
  void testWorksWellAfterCapBinds () {
    setFeeGrowthGlobal0X128(IntUtils.MAX_UINT256);
    burn(minTick, maxTick, ZERO);

    var position = positions(alice, minTick, maxTick);

    assertEquals(IntUtils.MAX_UINT128, position.tokensOwed0);
    assertEquals(ZERO, position.tokensOwed1);
  }
}