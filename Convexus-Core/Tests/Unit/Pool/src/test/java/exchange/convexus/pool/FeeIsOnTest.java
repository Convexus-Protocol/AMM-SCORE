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

import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.utils.IntUtils;

public class FeeIsOnTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];

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
    pool.invoke(owner, "setFeeProtocol", 6, 6);
  }

  @Test
  void testLimitSelling0For1AtTick0Thru1 () {
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("5981737760509663"));
    reset(sicx.spy);
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), 0, 120, TEN.pow(18));
    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger ("5981737760509663"), "{\"method\": \"pay\"}".getBytes());

    // somebody takes the limit order
    swapExact1For0(expandTo18Decimals(2), bob);
    reset(pool.spy);

    reset(sicx.spy);
    reset(usdc.spy);
    pool.invoke(alice, "burn", 0, 120, expandTo18Decimals(1));
    verify(pool.spy).Burn(alice.getAddress(), 0, 120, expandTo18Decimals(1), ZERO, new BigInteger("6017734268818165"));
    verifyNoInteractions(sicx.spy);
    verifyNoInteractions(usdc.spy);
    
    reset(pool.spy);
    reset(sicx.spy);
    pool.invoke(alice, "collect", alice.getAddress(), 0, 120, IntUtils.MAX_UINT256, IntUtils.MAX_UINT256);

    verify(usdc.spy).Transfer(
      pool.getAddress(), 
      alice.getAddress(), 
      // roughly 0.25% despite other liquidity
      // 6017734268818165 + 15089604485501
      new BigInteger("6032823873303666"), 
      "{\"method\": \"pay\"}".getBytes());

    assertTrue(Slot0.fromMap(pool.call("slot0")).tick >= 120);
  }

  @Test
  void testLimitSelling1For0AtTick0ThruMinus1 () {
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("5981737760509663"));

    reset(usdc.spy);
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), -120, 0, TEN.pow(18));
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("5981737760509663"), "{\"method\": \"pay\"}".getBytes());

    // somebody takes the limit order
    swapExact0For1(TEN.pow(18).multiply(TWO), bob);

    reset(sicx.spy);
    reset(usdc.spy);
    pool.invoke(alice, "burn", -120, 0, TEN.pow(18));
    verify(pool.spy).Burn(alice.getAddress(), -120, 0, TEN.pow(18), new BigInteger("6017734268818165"), ZERO);
    verifyNoInteractions(sicx.spy);
    verifyNoInteractions(usdc.spy);

    reset(pool.spy);
    reset(sicx.spy);
    pool.invoke(alice, "collect", alice.getAddress(), -120, 0, IntUtils.MAX_UINT256, IntUtils.MAX_UINT256);

    verify(sicx.spy).Transfer(
      pool.getAddress(), 
      alice.getAddress(), 
      // roughly 0.25% despite other liquidity
      // 6017734268818165 + 15089604485501
      new BigInteger("6032823873303666"), 
      "{\"method\": \"pay\"}".getBytes());
      
    assertTrue(Slot0.fromMap(pool.call("slot0")).tick < -120);
  }
}