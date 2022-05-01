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
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.ScoreSpy;
import static exchange.convexus.test.nft.NFTUtils.decreaseLiquidity;
import static exchange.convexus.test.nft.NFTUtils.mint;
import static exchange.convexus.utils.TimeUtils.now;

public class DecreaseLiquidityTest extends NonFungiblePositionManagerTest {

  final BigInteger tokenId = ONE;
  ScoreSpy<?> pool;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_nft();
    setup_initializer();

    // create a position
    pool = ConvexusTest.createAndInitializePoolIfNecessary(ConvexusPoolMock.class, alice, factory, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], encodePriceSqrt(ONE, ONE), tickSpacing);

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
      now().add(ONE)
    );
  }

  @Test
  void testFailsIfPastDeadline () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      decreaseLiquidity (
        nft,
        alice,
        tokenId,
        BigInteger.valueOf(50),
        ZERO,
        ZERO,
        now().subtract(ONE)
      ),
      "checkDeadline: Transaction too old"
    );
  }

  @Test
  void testCannotBeCalledByOtherAddresses () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      decreaseLiquidity (
        nft,
        bob,
        tokenId,
        BigInteger.valueOf(50),
        ZERO,
        ZERO,
        now().subtract(ONE)
      ),
      "checkAuthorizedForToken: Not approved"
    );
  }

  @Test
  void testDecreasesPositionLiquidity () {

    BigInteger balance0Before = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    BigInteger poolBalance0Before = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger depositedSicx = (BigInteger) nft.call("deposited", alice.getAddress(), sicx.getAddress());
    BigInteger depositedUsdc = (BigInteger) nft.call("deposited", alice.getAddress(), usdc.getAddress());

    score.Context.println("balance0Before = " + balance0Before);
    score.Context.println("depositedSicx = " + depositedSicx);
    score.Context.println("depositedUsdc = " + depositedUsdc);
    score.Context.println("poolBalance0Before = " + poolBalance0Before);

    decreaseLiquidity (
      nft,
      alice,
      tokenId,
      BigInteger.valueOf(25),
      ZERO,
      ZERO,
      now().add(ONE)
    );

    var position = PositionInformation.fromMap(nft.call("positions", tokenId));
    assertEquals(BigInteger.valueOf(75), position.liquidity);
    
    BigInteger balance0After = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    BigInteger poolBalance0After = (BigInteger) sicx.call("balanceOf", pool.getAddress());

    BigInteger depositedAfterSicx = (BigInteger) nft.call("deposited", alice.getAddress(), sicx.getAddress());
    BigInteger depositedAfterUsdc = (BigInteger) nft.call("deposited", alice.getAddress(), usdc.getAddress());
    score.Context.println("balance0After  = " + balance0After);
    score.Context.println("depositedAfterSicx = " + depositedAfterSicx);
    score.Context.println("depositedAfterUsdc = " + depositedAfterUsdc);
    score.Context.println("poolBalance0After = " + poolBalance0After);
  }

  @Test
  void testIsPayable () {
    // TODO
  }
  
  @Test
  void testAccountsForTokensOwed () {
    
    decreaseLiquidity (
      nft,
      alice,
      tokenId,
      BigInteger.valueOf(25),
      ZERO,
      ZERO,
      now().add(ONE)
    );

    var position = PositionInformation.fromMap(nft.call("positions", tokenId));
    assertEquals(BigInteger.valueOf(24), position.tokensOwed0);
    assertEquals(BigInteger.valueOf(24), position.tokensOwed1);
  }

  @Test
  void testCanDecreaseForAllTheLiquidity () {
    decreaseLiquidity (
      nft,
      alice,
      tokenId,
      BigInteger.valueOf(100),
      ZERO,
      ZERO,
      now().add(ONE)
    );
    
    var position = PositionInformation.fromMap(nft.call("positions", tokenId));
    assertEquals(BigInteger.valueOf(0), position.liquidity);
  }

  @Test
  void testCannotDecreaseForMoreThanAllTheLiquidity () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      decreaseLiquidity (
        nft,
        alice,
        tokenId,
        BigInteger.valueOf(101),
        ZERO,
        ZERO,
        now().add(ONE)
      ),
      "decreaseLiquidity: invalid liquidity"
    );
  }

  @Test
  void testCannotDecreaseForMoreThanTheLiquidityOfTheNftPosition () {

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
      now().add(ONE)
    );
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      decreaseLiquidity (
        nft,
        alice,
        tokenId,
        BigInteger.valueOf(101),
        ZERO,
        ZERO,
        now().add(ONE)
      ),
      "decreaseLiquidity: invalid liquidity"
    );
  }
}
