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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static exchange.convexus.NFTUtils.NFTUtils.mint;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.TimeUtils;
import score.Address;

public class CollectTest extends NonFungiblePositionManagerTest {

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
  void testCannotBeCalledByOtherAddresses () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      collect (
        bob,
        tokenId,
        bob.getAddress(),
        IntUtils.MAX_UINT128,
        IntUtils.MAX_UINT128
      ),
      "checkAuthorizedForToken: Not approved");
  }

  @Test
  void testCannotBeCalledWith0ForBothAmounts () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      collect (
        alice,
        tokenId,
        alice.getAddress(),
        ZERO,
        ZERO
      ),
      "collect: amount0Max and amount1Max cannot be both zero");
  }

  @Test
  void testNoOpIfNoTokensAreOwed () {
    reset(sicx.spy);
    reset(usdc.spy);

    collect (
      alice,
      tokenId,
      alice.getAddress(),
      IntUtils.MAX_UINT128,
      IntUtils.MAX_UINT128
    );

    verifyNoInteractions(sicx.spy);
    verifyNoInteractions(usdc.spy);
  }

  @Test
  void transfersTokensOwedFromBurn () {
    decreaseLiquidity(alice, tokenId, BigInteger.valueOf(50), ZERO, ZERO, TimeUtils.nowSeconds().add(ONE));
    Address poolAddress = (Address) factory.call("getPool", sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM]);

    reset(sicx.spy);
    reset(usdc.spy);

    collect (
      alice,
      tokenId,
      alice.getAddress(),
      IntUtils.MAX_UINT128,
      IntUtils.MAX_UINT128
    );

    verify(sicx.spy).Transfer(poolAddress, alice.getAddress(), BigInteger.valueOf(49), "collect".getBytes());
    verify(usdc.spy).Transfer(poolAddress, alice.getAddress(), BigInteger.valueOf(49), "collect".getBytes());
  }
}
