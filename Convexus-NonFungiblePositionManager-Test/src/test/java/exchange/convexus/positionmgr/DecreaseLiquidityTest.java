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

package exchange.convexus.positionmgr;

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.TimeUtils;
import static exchange.convexus.NFTUtils.NFTUtils.mint;

public class DecreaseLiquidityTest extends NonFungiblePositionManagerTest {

  final BigInteger tokenId = ONE;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_positionmgr();
    setup_initializer();

    // create a position
    createAndInitializePoolIfNecessary(sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], encodePriceSqrt(ONE, ONE), tickSpacing);

    final BigInteger hundred = BigInteger.valueOf(100);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), sicx.score, hundred);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), usdc.score, hundred);

    mint (
      nft,
      alice, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]),
      getMaxTick(TICK_SPACINGS[MEDIUM]),
      hundred, 
      hundred, 
      ZERO, 
      ZERO, 
      alice.getAddress(),
      TimeUtils.nowSeconds().add(ONE)
    );
  }

  @Test
  void testFailsIfPastDeadline () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      decreaseLiquidity (
        alice,
        tokenId,
        BigInteger.valueOf(50),
        ZERO,
        ZERO,
        TimeUtils.nowSeconds().subtract(ONE)
      ),
      "checkDeadline: Transaction too old"
    );
  }

  @Test
  void testCannotBeCalledByOtherAddresses () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      decreaseLiquidity (
        bob,
        tokenId,
        BigInteger.valueOf(50),
        ZERO,
        ZERO,
        TimeUtils.nowSeconds().subtract(ONE)
      ),
      "checkAuthorizedForToken: Not approved"
    );
  }

  @Test
  void testDecreasesPositionLiquidity () {
    decreaseLiquidity (
      alice,
      tokenId,
      BigInteger.valueOf(25),
      ZERO,
      ZERO,
      TimeUtils.nowSeconds().add(ONE)
    );

    var position = PositionInformation.fromMap(nft.call("positions", tokenId));
    assertEquals(BigInteger.valueOf(75), position.liquidity);
  }

  @Test
  void testIsPayable () {
    assertTrue(false); // TODO
  }
  
  @Test
  void testAccountsForTokensOwed () {
    
    decreaseLiquidity (
      alice,
      tokenId,
      BigInteger.valueOf(25),
      ZERO,
      ZERO,
      TimeUtils.nowSeconds().add(ONE)
    );

    var position = PositionInformation.fromMap(nft.call("positions", tokenId));
    assertEquals(BigInteger.valueOf(24), position.tokensOwed0);
    assertEquals(BigInteger.valueOf(24), position.tokensOwed1);
  }

  @Test
  void testCanDecreaseForAllTheLiquidity () {
    decreaseLiquidity (
      alice,
      tokenId,
      BigInteger.valueOf(100),
      ZERO,
      ZERO,
      TimeUtils.nowSeconds().add(ONE)
    );
    
    var position = PositionInformation.fromMap(nft.call("positions", tokenId));
    assertEquals(BigInteger.valueOf(0), position.liquidity);
  }

  @Test
  void testCannotDecreaseForMoreThanAllTheLiquidity () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      decreaseLiquidity (
        alice,
        tokenId,
        BigInteger.valueOf(101),
        ZERO,
        ZERO,
        TimeUtils.nowSeconds().add(ONE)
      ),
      "decreaseLiquidity: invalid liquidity"
    );
  }

  @Test
  void testCannotDecreaseForMOreThanTheLiquidityOfTheNftPosition () {

    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), sicx.score, BigInteger.valueOf(200));
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), usdc.score, BigInteger.valueOf(200));

    mint (
      nft,
      alice, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]),
      getMaxTick(TICK_SPACINGS[MEDIUM]),
      BigInteger.valueOf(200), 
      BigInteger.valueOf(200),
      ZERO, 
      ZERO, 
      alice.getAddress(),
      TimeUtils.nowSeconds().add(ONE)
    );
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      decreaseLiquidity (
        alice,
        tokenId,
        BigInteger.valueOf(101),
        ZERO,
        ZERO,
        TimeUtils.nowSeconds().add(ONE)
      ),
      "decreaseLiquidity: invalid liquidity"
    );
  }
}