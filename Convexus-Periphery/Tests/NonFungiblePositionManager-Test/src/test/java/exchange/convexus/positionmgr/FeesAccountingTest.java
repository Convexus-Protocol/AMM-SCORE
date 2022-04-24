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

package exchange.convexus.positionmgr;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static exchange.convexus.nft.NFTUtils.mint;
import static exchange.convexus.utils.IntUtils.MAX_UINT128;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.initializer.ConvexusPoolInitializerUtils;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.periphery.librairies.Path;
import exchange.convexus.pool.ConvexusPoolMock;
import exchange.convexus.pool.PoolData;
import exchange.convexus.router.SwapRouterUtils;
import static exchange.convexus.utils.TimeUtils.now;
import score.Address;

public class FeesAccountingTest extends NonFungiblePositionManagerTest {

  final BigInteger tokenId1 = ONE;
  final BigInteger tokenId2 = TWO;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_nft();
    setup_initializer();

    // create a position
    ConvexusPoolInitializerUtils.createAndInitializePoolIfNecessary(ConvexusPoolMock.class, alice, factory, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], encodePriceSqrt(ONE, ONE), tickSpacing);

    // nft 1 earns 25% of fees
    final BigInteger nft1Value = BigInteger.valueOf(100);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), sicx.score, nft1Value);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), usdc.score, nft1Value);
    mint (
      nft,
      alice, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]),
      getMaxTick(TICK_SPACINGS[MEDIUM]),
      nft1Value,
      nft1Value,
      ZERO, 
      ZERO, 
      alice.getAddress(),
      now().add(ONE)
    );
    
    // nft 2 earns 75% of fees
    final BigInteger nft2Value = BigInteger.valueOf(300);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), sicx.score, nft2Value);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), usdc.score, nft2Value);
    mint (
      nft,
      alice, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]),
      getMaxTick(TICK_SPACINGS[MEDIUM]),
      nft2Value,
      nft2Value,
      ZERO, 
      ZERO, 
      alice.getAddress(),
      now().add(ONE)
    );
    
    // swap for ~10k of fees
    var swapAmout = BigInteger.valueOf(3_333_333);
    SwapRouterUtils.exactInput(
      alice, 
      sicx.score, 
      router.getAddress(), 
      swapAmout,
      Path.encodePath(new PoolData(sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM])), 
      alice.getAddress(), 
      now().add(ONE), 
      ZERO
    );
  }

  @Test
  void testExpectedAmounts () {
    var result1 = collect(alice, tokenId1, alice.getAddress(), MAX_UINT128, MAX_UINT128);
    var result2 = collect(alice, tokenId2, alice.getAddress(), MAX_UINT128, MAX_UINT128);
    assertEquals(BigInteger.valueOf(2501), result1.amount0);
    assertEquals(BigInteger.valueOf(0), result1.amount1);
    assertEquals(BigInteger.valueOf(7503), result2.amount0);
    assertEquals(BigInteger.valueOf(0), result2.amount1);
  }

  @Test
  void testActuallyCollected () {
    Address pool = (Address) factory.call("getPool", sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM]);

    BigInteger before = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    reset(sicx.spy);
    reset(usdc.spy);
    collect(alice, tokenId1, alice.getAddress(), MAX_UINT128, MAX_UINT128);
    verify(sicx.spy).Transfer(pool, alice.getAddress(), BigInteger.valueOf(2501), "{\"method\": \"pay\"}".getBytes());
    verifyNoInteractions(usdc.spy);
    BigInteger after = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    assertEquals(after.subtract(before), BigInteger.valueOf(2501));

    before = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    reset(sicx.spy);
    reset(usdc.spy);
    collect(alice, tokenId2, alice.getAddress(), MAX_UINT128, MAX_UINT128);
    verify(sicx.spy).Transfer(pool, alice.getAddress(), BigInteger.valueOf(7503), "{\"method\": \"pay\"}".getBytes());
    verifyNoInteractions(usdc.spy);
    after = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    assertEquals(after.subtract(before), BigInteger.valueOf(7503));
  }
}