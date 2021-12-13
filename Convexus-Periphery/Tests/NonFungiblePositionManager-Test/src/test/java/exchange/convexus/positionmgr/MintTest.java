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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.initializer.ConvexusPoolInitializerUtils;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.pool.ConvexusPool;
import exchange.convexus.utils.AssertUtils;
import static exchange.convexus.utils.TimeUtils.now;
import static exchange.convexus.NFTUtils.NFTUtils.mint;

public class MintTest extends NonFungiblePositionManagerTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_nft();
    setup_initializer();
  }

  @Test
  void testMintFailsIfPoolDoesNotExist () {
    MintParams params = new MintParams();
    params.token0 = sicx.getAddress();
    params.token1 = usdc.getAddress();
    params.fee = FEE;
    params.tickLower = getMinTick(TICK_SPACINGS[MEDIUM]);
    params.tickUpper = getMaxTick(TICK_SPACINGS[MEDIUM]);
    params.amount0Desired = BigInteger.valueOf(100);
    params.amount1Desired = BigInteger.valueOf(100);
    params.amount0Min = ZERO;
    params.amount1Min = ZERO;
    params.recipient = alice.getAddress();
    params.deadline = now();

    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      mint (
        nft,
        alice,
        sicx.getAddress(),
        usdc.getAddress(),
        FEE,
        getMinTick(TICK_SPACINGS[MEDIUM]),
        getMaxTick(TICK_SPACINGS[MEDIUM]),
        BigInteger.valueOf(100),
        BigInteger.valueOf(100),
        ZERO,
        ZERO,
        alice.getAddress(),
        now()
      ),
      "addLiquidity: pool doesn't exist");
  }

  @Test
  void failsIfCannotTransfer () {
    ConvexusPoolInitializerUtils.createAndInitializePoolIfNecessary(ConvexusPool.class, alice, factory, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], encodePriceSqrt(ONE, ONE), tickSpacing);
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> 
      mint (
        nft,
        alice,
        sicx.getAddress(),
        usdc.getAddress(),
        FEE,
        getMinTick(TICK_SPACINGS[MEDIUM]),
        getMaxTick(TICK_SPACINGS[MEDIUM]),
        BigInteger.valueOf(100),
        BigInteger.valueOf(100),
        ZERO,
        ZERO,
        alice.getAddress(),
        now()
      ),
      "checkEnoughDeposited: user didn't deposit enough funds");
  }

  @Test
  void createsAToken () {
    ConvexusPoolInitializerUtils.createAndInitializePoolIfNecessary(ConvexusPool.class, alice, factory, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], encodePriceSqrt(ONE, ONE), tickSpacing);

    final BigInteger fifteen = BigInteger.valueOf(15);
    
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), sicx.score, fifteen);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), usdc.score, fifteen);

    mint (
      nft,
      alice,
      sicx.getAddress(),
      usdc.getAddress(),
      FEE_AMOUNTS[MEDIUM],
      getMinTick(TICK_SPACINGS[MEDIUM]),
      getMaxTick(TICK_SPACINGS[MEDIUM]),
      fifteen,
      fifteen,
      ZERO,
      ZERO,
      bob.getAddress(),
      now().add(BigInteger.TEN)
    );

    assertEquals(nft.call("balanceOf", bob.getAddress()), ONE);
    assertEquals(nft.call("tokenOfOwnerByIndex", bob.getAddress(), ZERO), ONE);

    var position = PositionInformation.fromMap(nft.call("positions", ONE));
    assertEquals(position.token0, sicx.getAddress());
    assertEquals(position.token1, usdc.getAddress());
    assertEquals(position.fee, FEE_AMOUNTS[MEDIUM]);
    assertEquals(position.tickLower, getMinTick(TICK_SPACINGS[MEDIUM]));
    assertEquals(position.tickUpper, getMaxTick(TICK_SPACINGS[MEDIUM]));
    assertEquals(position.liquidity, BigInteger.valueOf(15));
    assertEquals(position.tokensOwed0, BigInteger.valueOf(0));
    assertEquals(position.tokensOwed1, BigInteger.valueOf(0));
    assertEquals(position.feeGrowthInside0LastX128, BigInteger.valueOf(0));
    assertEquals(position.feeGrowthInside1LastX128, BigInteger.valueOf(0));
  }
}