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

import static exchange.convexus.librairies.TickMath.MAX_SQRT_RATIO;
import static exchange.convexus.librairies.TickMath.MIN_SQRT_RATIO;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.mockito.Mockito.reset;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.utils.AssertUtils;

public class SwapUnderpaymentTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;

  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];
  int tickLower = -TICK_SPACINGS[MEDIUM];
  int tickUpper = TICK_SPACINGS[MEDIUM];

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
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    mint(alice, minTick, maxTick, expandTo18Decimals(1), "1000000000000000000", "1000000000000000000");
  }

  @Test
  void testAmountSpecificedMustBeDifferentFromZero () {
    var slot0 = Slot0.fromMap(pool.call("slot0"));
    ConvexusLiquidityUtils.deposit(alice, underpay.getAddress(), sicx.score, ONE);
    ConvexusLiquidityUtils.deposit(alice, underpay.getAddress(), usdc.score, ONE);
    
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underpay.invoke(alice, "swap", pool.getAddress(), alice.getAddress(), false, slot0.sqrtPriceX96.add(ONE), ZERO, ONE, ONE),
      "swap: amountSpecified must be different from zero");
  }

  @Test
  void testUnderpayZeroForOneAndExactIn () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underswap_pay(alice, true, MIN_SQRT_RATIO.add(ONE), BigInteger.valueOf(1000), ONE, ZERO), 
      "swap: the callback didn't charge the payment (1)");
  }

  @Test
  void testPayInTheWrongTokenZeroForOneAndExactIn () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underswap_pay(alice, true, MIN_SQRT_RATIO.add(ONE), BigInteger.valueOf(1000), ZERO, BigInteger.valueOf(2000)), 
      "swap: the callback didn't charge the payment (1)");
  }

  @Test
  void testOverpayZeroForOneAndExactIn () {
   underswap_pay(alice, true, MIN_SQRT_RATIO.add(ONE), BigInteger.valueOf(1000), BigInteger.valueOf(2000), ZERO);
  }

  @Test
  void testUnderpayZeroForOneAndExactOut () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underswap_pay(alice, true, MIN_SQRT_RATIO.add(ONE), BigInteger.valueOf(-1000), ONE, ZERO), 
      "swap: the callback didn't charge the payment (1)");
  }

  @Test
  void testPayInTheWrongTokenZeroForOneAndExactOut () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underswap_pay(alice, true, MIN_SQRT_RATIO.add(ONE), BigInteger.valueOf(-1000), ZERO, BigInteger.valueOf(2000)), 
      "swap: the callback didn't charge the payment (1)");
  }

  @Test
  void testOverpayZeroForOneAndExactOut () {
   underswap_pay(alice, true, MIN_SQRT_RATIO.add(ONE), BigInteger.valueOf(-1000), BigInteger.valueOf(2000), ZERO);
  }

  @Test
  void testUnderpayONeForZeroAndExactIn () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underswap_pay(alice, false, MAX_SQRT_RATIO.subtract(ONE), BigInteger.valueOf(1000), ZERO, BigInteger.valueOf(1)), 
      "swap: the callback didn't charge the payment (2)");
  }

  @Test
  void testPayInTheWrongTokenONeForZeroAndExactIn () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underswap_pay(alice, false, MAX_SQRT_RATIO.subtract(ONE), BigInteger.valueOf(1000), BigInteger.valueOf(2000), ZERO), 
      "swap: the callback didn't charge the payment (2)");
  }

  @Test
  void testOverpayOneForZeroAndExactIn () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underswap_pay(alice, false, MAX_SQRT_RATIO.subtract(ONE), BigInteger.valueOf(1), BigInteger.valueOf(1000), BigInteger.valueOf(2000)), 
      "swap: the callback didn't charge the payment (2)");
  }

  @Test
  void testUnderpayOneForZeroAndExactOut () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underswap_pay(alice, false, MAX_SQRT_RATIO.subtract(ONE), BigInteger.valueOf(-1000), ZERO, BigInteger.valueOf(1)), 
      "swap: the callback didn't charge the payment (2)");
  }

  @Test
  void testPayInTheWrongTokenOneForZeroAndExactOut () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> underswap_pay(alice, false, MAX_SQRT_RATIO.subtract(ONE), BigInteger.valueOf(-1000), BigInteger.valueOf(2000), ZERO), 
      "swap: the callback didn't charge the payment (2)");
  }

  @Test
  void testOverpayOneForZeroAndExactOut () {
   underswap_pay(alice, false, MAX_SQRT_RATIO.subtract(ONE), BigInteger.valueOf(-1000), ZERO, BigInteger.valueOf(2000));
  }
}