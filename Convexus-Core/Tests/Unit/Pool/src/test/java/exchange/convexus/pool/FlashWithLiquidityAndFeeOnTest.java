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
import static java.math.BigInteger.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.test.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.AddressUtils;

public class FlashWithLiquidityAndFeeOnTest extends ConvexusPoolTest {

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

    pool.invoke(owner, "setFeeProtocol", 6, 6);
  }

  @Test
  void testEmitsAnEvent () {
    reset(pool.spy);
    flash(alice, "1001", "2001", bob, "1005", "2008");
    verify(pool.spy).Flash(callee.getAddress(), bob.getAddress(), new BigInteger("1001"), new BigInteger("2001"), new BigInteger("4"), new BigInteger("7"));
  }

  @Test
  void testIncreasesTheFeeGrowthByTheExpectedAmount () {
    flash(alice, "2002", "4004", bob, "2009", "4017");

    var fees = ProtocolFees.fromMap(pool.call("protocolFees"));
    assertEquals(ONE, fees.token0);
    assertEquals(TWO, fees.token1);

    BigInteger expectedFee0 = BigInteger.valueOf(6).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee0, pool.call("feeGrowthGlobal0X128"));
    
    BigInteger expectedFee1 = BigInteger.valueOf(11).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee1, pool.call("feeGrowthGlobal1X128"));
  }

  @Test
  void testAllowsDonatingToken0 () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, "0", "0", AddressUtils.ZERO_ADDRESS, "567", "0");
    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("567"), "{\"method\": \"deposit\"}".getBytes());
    verify(usdc.spy, times(2)).balanceOf(pool.getAddress());

    var fees = ProtocolFees.fromMap(pool.call("protocolFees"));

    assertEquals(fees.token0, new BigInteger("94"));
    BigInteger expectedFee0 = BigInteger.valueOf(473).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee0, pool.call("feeGrowthGlobal0X128"));
  }
  
  @Test
  void testAllowsDonatingToken1 () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, "0", "0", AddressUtils.ZERO_ADDRESS, "0", "678");
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("678"), "{\"method\": \"deposit\"}".getBytes());
    verify(sicx.spy, times(2)).balanceOf(pool.getAddress());

    var fees = ProtocolFees.fromMap(pool.call("protocolFees"));

    assertEquals(fees.token1, new BigInteger("113"));
    BigInteger expectedFee1 = BigInteger.valueOf(565).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee1, pool.call("feeGrowthGlobal1X128"));
  }

  @Test
  void testAllowsDonatingToken0AndToken1Together () {
    reset(pool.spy);
    reset(sicx.spy);
    reset(usdc.spy);
    flash(alice, "0", "0", bob, "789", "1234");
    verify(sicx.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("789"), "{\"method\": \"deposit\"}".getBytes());
    verify(usdc.spy).Transfer(callee.getAddress(), pool.getAddress(), new BigInteger("1234"), "{\"method\": \"deposit\"}".getBytes());
    
    var fees = ProtocolFees.fromMap(pool.call("protocolFees"));
    assertEquals(fees.token0, new BigInteger("131"));
    assertEquals(fees.token1, new BigInteger("205"));

    BigInteger expectedFee0 = BigInteger.valueOf(658).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee0, pool.call("feeGrowthGlobal0X128"));
    BigInteger expectedFee1 = BigInteger.valueOf(1029).multiply(TWO.pow(128)).divide(expandTo18Decimals(2));
    assertEquals(expectedFee1, pool.call("feeGrowthGlobal1X128"));
  }
}
