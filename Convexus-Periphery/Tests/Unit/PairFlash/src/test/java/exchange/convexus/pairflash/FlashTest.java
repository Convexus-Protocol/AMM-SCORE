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

package exchange.convexus.pairflash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;

import java.math.BigInteger;


import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;

public class FlashTest extends PairFlashTest {
  
  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_pairflash();
    setup_positionmgr();
    setup_pool1();
    setup_pool2();
    setup_pool3();
  }

  @Test
  void testCorrectTransfer () {
    // choose amountIn to test
    var amount0In = BigInteger.valueOf(1000);
    var amount1In = BigInteger.valueOf(1000);

    BigInteger fee0 = amount0In.multiply(BigInteger.valueOf(FEE_AMOUNTS[MEDIUM])).divide(BigInteger.valueOf(1000000));
    BigInteger fee1 = amount1In.multiply(BigInteger.valueOf(FEE_AMOUNTS[MEDIUM])).divide(BigInteger.valueOf(1000000));

    var expectedAmountOut0 = quoteExactInputSingle(alice, usdc.getAddress(), sicx.getAddress(), amount1In, FEE_AMOUNTS[LOW], encodePriceSqrt(20, 10));
    var expectedAmountOut1 = quoteExactInputSingle(alice, sicx.getAddress(), usdc.getAddress(), amount0In, FEE_AMOUNTS[HIGH], encodePriceSqrt(5, 10));

    ConvexusLiquidityUtils.deposit(alice, flash.getAddress(), sicx.score, BigInteger.valueOf(1000));
    ConvexusLiquidityUtils.deposit(alice, flash.getAddress(), usdc.score, BigInteger.valueOf(1000));

    BigInteger before0 = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    BigInteger before1 = (BigInteger) usdc.call("balanceOf", alice.getAddress());
    reset(sicx.spy);
    reset(usdc.spy);
    initFlash(alice, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], amount0In, amount1In, FEE_AMOUNTS[LOW], FEE_AMOUNTS[HIGH]);
    BigInteger after0 = (BigInteger) sicx.call("balanceOf", alice.getAddress());
    BigInteger after1 = (BigInteger) usdc.call("balanceOf", alice.getAddress());
    BigInteger profit0 = after0.subtract(before0);
    BigInteger profit1 = after1.subtract(before1);

    assertEquals(expectedAmountOut0.amountOut.subtract(amount0In).subtract(fee0), profit0);
    assertEquals(expectedAmountOut1.amountOut.subtract(amount1In).subtract(fee1), profit1);
  }
}
