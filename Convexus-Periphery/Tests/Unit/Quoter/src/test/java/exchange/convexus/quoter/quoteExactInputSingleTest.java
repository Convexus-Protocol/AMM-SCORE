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

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.clients.QuoterClient;
import exchange.convexus.test.ConvexusTest;
import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.test.nft.NFTUtils;
import exchange.convexus.periphery.quoter.QuoteExactInputSingleParams;
import exchange.convexus.mocks.pool.ConvexusPoolMock;
import exchange.convexus.pool.Slot0;
import exchange.convexus.test.swaprouter.SwapRouterUtils;
import exchange.convexus.utils.ScoreSpy;
import exchange.convexus.utils.TimeUtils;
import score.Address;

public class quoteExactInputSingleTest extends QuoterTest {

  ScoreSpy<ConvexusPoolMock> pool;
  
  @BeforeEach
  @SuppressWarnings("unchecked")
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_quoter();
    
    // create a position
    setup_tokens();
    setup_nft();
    setup_router();
    
    pool = (ScoreSpy<ConvexusPoolMock>) ConvexusTest.createAndInitializePoolIfNecessary(ConvexusPoolMock.class, alice, factory, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], encodePriceSqrt(ONE, ONE), tickSpacing);

    // Mint a position
    final BigInteger thousand = EXA.multiply(BigInteger.valueOf(1000));
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), sicx.score, thousand);
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), usdc.score, thousand);

    NFTUtils.mint (
      nft,
      alice, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]),
      getMaxTick(TICK_SPACINGS[MEDIUM]),
      thousand, 
      thousand, 
      ZERO, 
      ZERO, 
      alice.getAddress(),
      TimeUtils.now().add(ONE)
    );
  }

  @Test
  void testQuoteExactInputSingle () {
    Address tokenIn = sicx.getAddress();
    Address tokenOut = usdc.getAddress();
    BigInteger amountIn = BigInteger.valueOf(1000);
    int fee = FEE_AMOUNTS[MEDIUM];
    BigInteger sqrtPriceLimitX96 = encodePriceSqrt(ONE, BigInteger.TWO);
    
    QuoteExactInputSingleParams params = new QuoteExactInputSingleParams(
      tokenIn,
      tokenOut,
      amountIn,
      fee,
      sqrtPriceLimitX96
    );

    Slot0 slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(new BigInteger("1000000000000000000000000", 16), slot0.sqrtPriceX96);

    var quote = QuoterClient.quoteExactInputSingle(quoter, alice, params);
    assertEquals(0, quote.initializedTicksCrossed);
    assertEquals(new BigInteger("996"), quote.amountOut);
    assertEquals(new BigInteger("ffffffffffffffed9bccf536", 16), quote.sqrtPriceX96After);

    // make sure it's working as expected
    SwapRouterUtils.exactInputSingle(alice, sicx.score, router.getAddress(), amountIn, usdc.getAddress(), fee, alice.getAddress(), TimeUtils.now().add(TimeUtils.ONE_HOUR), quote.amountOut, quote.sqrtPriceX96After);
    slot0 = Slot0.fromMap(pool.call("slot0"));
    assertEquals(slot0.sqrtPriceX96, quote.sqrtPriceX96After);
  }
}