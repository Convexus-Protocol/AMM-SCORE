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

package exchange.convexus.pool;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.IntUtils;
import score.Address;

class CollectOverflowTest extends ConvexusPoolTest {

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
    setup_pool(factory.getAddress(), FEE, tickSpacing);
    reset(pool.spy);

    // Transfer some funds to Alice
    sicx.invoke(owner, "mintTo", alice.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));
    usdc.invoke(owner, "mintTo", alice.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));
    // Transfer some funds to Bob
    sicx.invoke(owner, "mintTo", bob.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));
    usdc.invoke(owner, "mintTo", bob.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));

    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE, pool.getAddress());
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    
    setFeeGrowthGlobal0X128(IntUtils.MAX_UINT256);
    setFeeGrowthGlobal1X128(IntUtils.MAX_UINT256);
    mint(alice, minTick, maxTick, expandTo18Decimals(10), "10000000000000000000", "10000000000000000000");
  }

  @Test
  void testToken0 () {
    swapExact0For1(expandTo18Decimals(1), alice, "1000000000000000000");
    burn(minTick, maxTick, ZERO);

    reset(pool.spy);
    pool.invoke(alice, "collect", alice.getAddress(), minTick, maxTick, IntUtils.MAX_UINT128, IntUtils.MAX_UINT128);

    // Get Collect event
    ArgumentCaptor<Address> _caller = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _tickLower = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickUpper = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(pool.spy).Collect(_caller.capture(), _tickLower.capture(), _tickUpper.capture(), _recipient.capture(), _amount0.capture(), _amount1.capture());

    assertEquals(new BigInteger("499999999999999"), _amount0.getValue());
    assertEquals(BigInteger.valueOf(0), _amount1.getValue());
  }
  
  @Test
  void testToken1 () {
    swapExact1For0(expandTo18Decimals(1), alice, "1000000000000000000");
    burn(minTick, maxTick, ZERO);

    reset(pool.spy);
    pool.invoke(alice, "collect", alice.getAddress(), minTick, maxTick, IntUtils.MAX_UINT128, IntUtils.MAX_UINT128);

    // Get Collect event
    ArgumentCaptor<Address> _caller = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _tickLower = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickUpper = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(pool.spy).Collect(_caller.capture(), _tickLower.capture(), _tickUpper.capture(), _recipient.capture(), _amount0.capture(), _amount1.capture());

    assertEquals(ZERO, _amount0.getValue());
    assertEquals(new BigInteger("499999999999999"), _amount1.getValue());
  }

  @Test
  void testToken0AndToken1 () {
    swapExact0For1(expandTo18Decimals(1), alice, "1000000000000000000");
    swapExact1For0(expandTo18Decimals(1), alice, "1000000000000000000");
    burn(minTick, maxTick, ZERO);

    reset(pool.spy);
    pool.invoke(alice, "collect", alice.getAddress(), minTick, maxTick, IntUtils.MAX_UINT128, IntUtils.MAX_UINT128);

    // Get Collect event
    ArgumentCaptor<Address> _caller = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _tickLower = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickUpper = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(pool.spy).Collect(_caller.capture(), _tickLower.capture(), _tickUpper.capture(), _recipient.capture(), _amount0.capture(), _amount1.capture());

    assertEquals(new BigInteger("499999999999999"), _amount0.getValue());
    assertEquals(new BigInteger("500000000000000"), _amount1.getValue());
  }
}
