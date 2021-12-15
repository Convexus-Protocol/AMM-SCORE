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
import static exchange.convexus.nft.NFTUtils.mint;
import static exchange.convexus.utils.AddressUtils.ZERO_ADDRESS;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.initializer.ConvexusPoolInitializerUtils;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.pool.ConvexusPool;
import exchange.convexus.utils.AssertUtils;
import static exchange.convexus.utils.TimeUtils.now;

public class TransferFromTest extends NonFungiblePositionManagerTest {

  final BigInteger tokenId = ONE;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_nft();
    setup_initializer();

    // create a position
    ConvexusPoolInitializerUtils.createAndInitializePoolIfNecessary(ConvexusPool.class, alice, factory, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], encodePriceSqrt(ONE, ONE), tickSpacing);

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
      bob.getAddress(),
      now().add(ONE)
    );
  }
  
  @Test
  void testCanOnlyBeCalledByAuthorizedOrOwner () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      transferFrom(alice, bob.getAddress(), alice.getAddress(), tokenId),
      "IRC721: transfer caller is not owner nor approved");
  }

  @Test
  void testChangesTheOwner () {
    transferFrom(bob, bob.getAddress(), alice.getAddress(), tokenId);
    assertEquals(alice.getAddress(), nft.call("ownerOf", tokenId));
  }

  @Test
  void testRemovesExistingApproval () {
    approve(bob, alice.getAddress(), tokenId);
    assertEquals(alice.getAddress(), nft.call("getApproved", tokenId));
    transferFrom(alice, bob.getAddress(), alice.getAddress(), tokenId);
    assertEquals(ZERO_ADDRESS, nft.call("getApproved", tokenId));
  }

}
