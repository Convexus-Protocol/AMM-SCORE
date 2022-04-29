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

package exchange.convexus.positionmgr;

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.test.ConvexusTest;
import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.mocks.pool.ConvexusPoolMock;

import static exchange.convexus.test.nft.NFTUtils.mint;
import static exchange.convexus.utils.TimeUtils.now;

public class IncreaseLiquidityTest extends NonFungiblePositionManagerTest {

  final BigInteger tokenId = ONE;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_nft();
    setup_initializer();

    // create a position
    ConvexusTest.createAndInitializePoolIfNecessary(ConvexusPoolMock.class, alice, factory, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], encodePriceSqrt(ONE, ONE), tickSpacing);

    final BigInteger thousand = BigInteger.valueOf(1000);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), sicx.score, thousand);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), usdc.score, thousand);

    mint (
      nft,
      alice, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]),
      getMaxTick(TICK_SPACINGS[MEDIUM]),
      thousand, 
      thousand, 
      ZERO, 
      ZERO, 
      alice.getAddress(),
      now().add(ONE)
    );
  }

  @Test
  void testIncreasesPositionLiquidity () {
    final BigInteger thousand = BigInteger.valueOf(1000);
    final BigInteger hundred  = BigInteger.valueOf(100);

    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), sicx.score, hundred);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), usdc.score, hundred);

    increaseLiquidity (
      alice,
      tokenId,
      hundred,
      hundred,
      ZERO,
      ZERO,
      now().add(ONE)
    );

    var position = PositionInformation.fromMap(nft.call("positions", tokenId));
    assertEquals(thousand.add(hundred), position.liquidity);
  }

  @Test
  void testCanBePaidWithIcx () {
    // TODO
  }
}
