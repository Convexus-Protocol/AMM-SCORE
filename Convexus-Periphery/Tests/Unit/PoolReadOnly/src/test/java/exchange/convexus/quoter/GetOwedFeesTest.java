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

package exchange.convexus.quoter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static java.math.BigInteger.*;

import java.math.BigInteger;
import java.util.Map;
import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.test.factory.ConvexusFactoryUtils;
import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;

public class GetOwedFeesTest extends PoolReadOnlyTest {
  
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
    setup_nft();
    setup_poolreadonly();
    
    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE, pool.getAddress());
    initializeAtZeroTick();
  }

  @Test
  void testLimitSelling1For0AtTick0ThruMinus1 () {
    int tickLower = -120;
    int tickUpper = 0;
    
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("5981737760509663"));
    BigInteger liquidity = TEN.pow(18);

    reset(usdc.spy);
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), tickLower, tickUpper, liquidity);
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("5981737760509663"), "{\"method\": \"deposit\"}".getBytes());

    // somebody else takes the limit order
    swapExact0For1(liquidity.multiply(TWO), bob);

    @SuppressWarnings("unchecked")
    Map<String, BigInteger> owed = (Map<String, BigInteger>) poolReadonly.call("getOwedFees", alice.getAddress(), pool.getAddress(), tickLower, tickUpper);
    
    assertEquals((BigInteger) owed.get("amount0"), new BigInteger("18107525382602"));
  }
}