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

import com.eclipsesource.json.Json;
import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.test.ConvexusTest;
import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.mocks.pool.ConvexusPoolMock;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.StringUtils;

import static exchange.convexus.test.nft.NFTUtils.mint;
import static exchange.convexus.utils.TimeUtils.now;

public class TokenURITest extends NonFungiblePositionManagerTest {

  final BigInteger tokenId = ONE;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_nft();
    setup_initializer();

    // create a position
    ConvexusTest.createAndInitializePoolIfNecessary(ConvexusPoolMock.class, alice, factory, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], encodePriceSqrt(ONE, ONE), tickSpacing);

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
  void testRevertsForINvalidTokenId () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      nft.call("tokenURI", tokenId.add(ONE)), 
      "positions: Invalid token ID");
  }

  @Test
  void testContentIsValidJSONAndStructure () {
    var tokenUriJson = (String) nft.call("tokenURI", tokenId);
    var tokenUri = Json.parse(tokenUriJson);
    assertEquals(tokenId, StringUtils.toBigInt(tokenUri.asObject().get("tokenId").asString()));
  }
}
