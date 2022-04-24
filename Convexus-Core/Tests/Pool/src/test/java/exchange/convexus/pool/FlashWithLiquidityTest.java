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

package exchange.convexus.pool;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.AssertUtils;

public class FlashWithLiquidityTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];

  int minTick = getMinTick(tickSpacing);
  int maxTick = getMaxTick(tickSpacing);

  BigInteger balance0;
  BigInteger balance1;

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

    balance0 = (BigInteger) sicx.score.call("balanceOf", pool.getAddress());
    balance1 = (BigInteger) usdc.score.call("balanceOf", pool.getAddress());
  }

  @Test
  void testEmitsAnEvent () {
    reset(pool.spy);
    flash(alice, "1001", "2001", bob, "1005", "2008");
    verify(pool.spy).Flash(callee.getAddress(), bob.getAddress(), new BigInteger("1001"), new BigInteger("2001"), new BigInteger("4"), new BigInteger("7"));
  }

  @Test
  void testTransfersTheAmount0ToTheRecipient () {
    reset(pool.spy);
    reset(sicx.spy);
    flash(alice, "100", "200", bob, "101", "201");
    verify(sicx.spy).Transfer(pool.getAddress(), bob.getAddress(), new BigInteger("100"), "{\"method\": \"pay\"}".getBytes());
  }
  
  @Test
  void testTransfersTheAmount1ToTheRecipient () {
    reset(pool.spy);
    reset(usdc.spy);
    flash(alice, "100", "200", bob, "101", "201");
    verify(usdc.spy).Transfer(pool.getAddress(), bob.getAddress(), new BigInteger("200"), "{\"method\": \"pay\"}".getBytes());
  }

  @Test
  void testCanFlashOnlyToken0 () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, "101", "0", bob, "102", "0");
    verify(sicx.spy).Transfer(pool.getAddress(), bob.getAddress(), new BigInteger("101"), "{\"method\": \"pay\"}".getBytes());
    verify(usdc.spy, times(2)).balanceOf(pool.getAddress());
  }

  @Test
  void testCanFlashOnlyToken1 () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, "0", "102", bob, "0", "103");
    verify(usdc.spy).Transfer(pool.getAddress(), bob.getAddress(), new BigInteger("102"), "{\"method\": \"pay\"}".getBytes());
    verify(sicx.spy, times(2)).balanceOf(pool.getAddress());
  }

  @Test
  void testCanFlashEntireTokenBalance () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, balance0, balance1, bob, "2006000000000000000", "2006000000000000000");
    
    verify(sicx.spy).Transfer(pool.getAddress(), bob.getAddress(), balance0, "{\"method\": \"pay\"}".getBytes());
    verify(usdc.spy).Transfer(pool.getAddress(), bob.getAddress(), balance1, "{\"method\": \"pay\"}".getBytes());
  }

  @Test
  void testNoOpIfBothAmountsAre0 () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, ZERO, ZERO, bob, "0", "0");
    verify(sicx.spy, times(2)).balanceOf(pool.getAddress());
    verify(usdc.spy, times(2)).balanceOf(pool.getAddress());
  }

  @Test
  void testFailsIfFlashAmountIsGreaterThanTokenBalance1 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> flash(alice, balance0.add(ONE), balance1, bob, "0", "0"),
      "Insufficient balance");
  }
  
  @Test
  void testFailsIfFlashAmountIsGreaterThanTokenBalance2 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> flash(alice, balance0, balance1.add(ONE), bob, "0", "0"),
      "Insufficient balance");
  }

  @Test
  void testCallsTheFlashCallbackOnTheSenderWithCorrectFeeAmounts () {
    reset(pool.spy);
    flash(alice, "1001", "2002", bob, "1005", "2009");
    verify(callee.spy).FlashCallback(BigInteger.valueOf(4), BigInteger.valueOf(7));
  }

  @Test
  void testIncreasesTheFeeGrowthByTheExpectedAmount () {
    flash(alice, "1001", "2002", bob, "1005", "2009");
    BigInteger expectedFee0 = BigInteger.valueOf(4).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee0, pool.call("feeGrowthGlobal0X128"));
    
    BigInteger expectedFee1 = BigInteger.valueOf(7).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee1, pool.call("feeGrowthGlobal1X128"));
  }

  @Test
  void testFailsIfOriginalBalanceNotReturnedInEitherToken0 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
    () -> flash(alice, "1000", "0", bob, "999", "0"), 
    "flash: not enough token0 returned");
  }
  
  @Test
  void testFailsIfOriginalBalanceNotReturnedInEitherToken1 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
    () -> flash(alice, "0", "1000", bob, "0", "999"), 
    "flash: not enough token1 returned");
  }

  @Test
  void testFailsIfUnderpaysEitherToken0 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
    () -> flash(alice, "1000", "0", bob, "1002", "0"), 
    "flash: not enough token0 returned");
  }
  
  @Test
  void testFailsIfUnderpaysEitherToken1 () {
    AssertUtils.assertThrowsMessage(AssertionError.class, 
    () -> flash(alice, "0", "1000", bob, "0", "1002"), 
    "flash: not enough token1 returned");
  }

  @Test
  void testAllowsDonatingToken0 () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, "0", "0", bob, "567", "0");
    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("567"), "{\"method\": \"pay\"}".getBytes());
    verify(usdc.spy, times(2)).balanceOf(pool.getAddress());
    
    BigInteger expectedFee0 = BigInteger.valueOf(567).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee0, pool.call("feeGrowthGlobal0X128"));
  }

  @Test
  void testAllowsDonatingToken1 () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, "0", "0", bob, "0", "678");
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("678"), "{\"method\": \"pay\"}".getBytes());
    verify(sicx.spy, times(2)).balanceOf(pool.getAddress());
    
    BigInteger expectedFee1 = BigInteger.valueOf(678).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee1, pool.call("feeGrowthGlobal1X128"));
  }

  @Test
  void testAllowsDonatingToken0AndToken1Together () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, "0", "0", bob, "789", "1234");
    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("789"), "{\"method\": \"pay\"}".getBytes());
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("1234"), "{\"method\": \"pay\"}".getBytes());
    
    BigInteger expectedFee0 = BigInteger.valueOf(789).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee0, pool.call("feeGrowthGlobal0X128"));
    BigInteger expectedFee1 = BigInteger.valueOf(1234).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee1, pool.call("feeGrowthGlobal1X128"));
  }
}
