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

package exchange.convexus.pool;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.test.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.IntUtils;
import score.Address;

public class MintMiscTest extends ConvexusPoolTest {

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
    initializeAtZeroTick();
  }

  @Test
  void testMintToTheRightOfTheCurrentPrice() {
    BigInteger liquidityDelta = BigInteger.valueOf(1000);
    int lowerTick = tickSpacing;
    int upperTick = tickSpacing * 2;

    BigInteger liquidityBefore = (BigInteger) pool.call("liquidity");

    BigInteger b0 = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger b1 = (BigInteger) usdc.call("balanceOf", pool.getAddress());

    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), lowerTick, upperTick, liquidityDelta);
    
    BigInteger liquidityAfter = (BigInteger) pool.call("liquidity");
    assertTrue(liquidityAfter.compareTo(liquidityBefore) >= 0);

    BigInteger b0After = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger b1After = (BigInteger) usdc.call("balanceOf", pool.getAddress());
    
    assertEquals(b0After.subtract(b0), ONE);
    assertEquals(b1After.subtract(b1), ZERO);
  }

  @Test
  void testMintToTheLeftOfTheCurrentPrice () {
    BigInteger liquidityDelta = BigInteger.valueOf(1000);
    int lowerTick = -tickSpacing * 2;
    int upperTick = -tickSpacing;
    
    BigInteger liquidityBefore = (BigInteger) pool.call("liquidity");

    BigInteger b0 = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger b1 = (BigInteger) usdc.call("balanceOf", pool.getAddress());

    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), lowerTick, upperTick, liquidityDelta);
    
    BigInteger liquidityAfter = (BigInteger) pool.call("liquidity");
    assertTrue(liquidityAfter.compareTo(liquidityBefore) >= 0);

    BigInteger b0After = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger b1After = (BigInteger) usdc.call("balanceOf", pool.getAddress());
    
    assertEquals(b0After.subtract(b0), ZERO);
    assertEquals(b1After.subtract(b1), ONE);
  }

  @Test
  void testMintWithinTheCurrentPrice () {
    BigInteger liquidityDelta = BigInteger.valueOf(1000);
    int lowerTick = -tickSpacing;
    int upperTick = tickSpacing;
    
    BigInteger liquidityBefore = (BigInteger) pool.call("liquidity");

    BigInteger b0 = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger b1 = (BigInteger) usdc.call("balanceOf", pool.getAddress());

    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("1"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("1"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), lowerTick, upperTick, liquidityDelta);
    
    BigInteger liquidityAfter = (BigInteger) pool.call("liquidity");
    assertTrue(liquidityAfter.compareTo(liquidityBefore) >= 0);

    BigInteger b0After = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger b1After = (BigInteger) usdc.call("balanceOf", pool.getAddress());
    
    assertEquals(b0After.subtract(b0), ONE);
    assertEquals(b1After.subtract(b1), ONE);
  }

  @Test
  void testCannotRemoveMOreThanTheEntirePosition () {
    int lowerTick = -tickSpacing;
    int upperTick = tickSpacing;

    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("499850034993001260"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("499850034993001260"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), lowerTick, upperTick, TEN.pow(18).multiply(BigInteger.valueOf(1000)));

    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      pool.invoke(alice, "burn", lowerTick, upperTick, TEN.pow(18).multiply(BigInteger.valueOf(1001))),
      "addDelta: z < x");
  }

  @Test
  void testCollectFeesWithinTheCurrentPriceAfterSwap () {
    BigInteger liquidityDelta = TEN.pow(18).multiply(BigInteger.valueOf(100));
    int lowerTick = -tickSpacing * 100;
    int upperTick = tickSpacing * 100;
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("4876819758127888900"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("4876819758127888900"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), lowerTick, upperTick, liquidityDelta);
    
    BigInteger liquidityBefore = (BigInteger) pool.call("liquidity");

    BigInteger amount0In = TEN.pow(18);
    swapExact0For1(amount0In, alice);
    
    BigInteger liquidityAfter = (BigInteger) pool.call("liquidity");
    assertTrue(liquidityAfter.compareTo(liquidityBefore) >= 0);
    
    BigInteger token0BalanceBeforePool = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger token1BalanceBeforePool = (BigInteger) usdc.call("balanceOf", pool.getAddress());
    BigInteger token0BalanceBeforeAlice = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    BigInteger token1BalanceBeforeAlice = (BigInteger) usdc.call("balanceOf", alice.getAddress());
    
    pool.invoke(alice, "burn", lowerTick, upperTick, ZERO);
    pool.invoke(alice, "collect", alice.getAddress(), lowerTick, upperTick, IntUtils.MAX_UINT256, IntUtils.MAX_UINT256);
    
    pool.invoke(alice, "burn", lowerTick, upperTick, ZERO);
    reset(pool.spy);
    pool.invoke(alice, "collect", alice.getAddress(), lowerTick, upperTick, IntUtils.MAX_UINT256, IntUtils.MAX_UINT256);
    
    // Get Collect event
    ArgumentCaptor<Address> _caller = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _tickLower = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickUpper = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(pool.spy).Collect(_caller.capture(), _tickLower.capture(), _tickUpper.capture(), _recipient.capture(), _amount0.capture(), _amount1.capture());
    
    assertEquals(ZERO, _amount0.getValue());
    assertEquals(ZERO, _amount1.getValue());
    
    BigInteger token0BalanceAfterAlice = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    BigInteger token1BalanceAfterAlice = (BigInteger) usdc.call("balanceOf", alice.getAddress());
    BigInteger token0BalanceAfterPool = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger token1BalanceAfterPool = (BigInteger) usdc.call("balanceOf", pool.getAddress());

    assertTrue(token0BalanceAfterAlice.compareTo(token0BalanceBeforeAlice) > 0);
    assertTrue(token1BalanceAfterAlice.equals(token1BalanceBeforeAlice));

    assertTrue(token0BalanceAfterPool.compareTo(token0BalanceBeforePool) < 0);
    assertTrue(token1BalanceAfterPool.equals(token1BalanceBeforePool));
  }
}