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

package exchange.convexus.router;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.test.contracts.pool.Pool1;
import exchange.convexus.test.contracts.pool.Pool2;
import exchange.convexus.utils.ICX;
import score.Address;

public class ExactInputSingleIcxTest extends SwapRouterTest {

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_router();
    setup_nft(factory.getAddress());
      
    createIcxPool(Pool1.class, usdc.score);
    createPool(Pool2.class, usdc.score, baln.score);
  }

  @Test
  void testSwap01 () {
    Address pool = (Address) factory.call("getPool", ICX.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM]);

    // get balances before
    BigInteger[] poolBefore = getBalancesIcx(pool);
    BigInteger[] traderBefore = getBalancesIcx(trader.getAddress());

    exactInputSingleIcx(usdc.score);

    // get balances after
    BigInteger[] poolAfter = getBalancesIcx(pool);
    BigInteger[] traderAfter = getBalancesIcx(trader.getAddress());

    assertEquals(traderAfter[0], traderBefore[0].subtract(BigInteger.valueOf(3)));
    assertEquals(traderAfter[1], traderBefore[1].add(BigInteger.valueOf(1)));
    assertEquals(poolAfter[0], poolBefore[0].add(BigInteger.valueOf(3)));
    assertEquals(poolAfter[1], poolBefore[1].subtract(BigInteger.valueOf(1)));
  }
  
  @Test
  void testSwap10 () {
    Address pool = (Address) factory.call("getPool", usdc.getAddress(), ICX.getAddress(), FEE_AMOUNTS[MEDIUM]);

    // get balances before
    BigInteger[] poolBefore = getBalancesIcx(pool);
    BigInteger[] traderBefore = getBalancesIcx(trader.getAddress());
    
    exactInputSingleToIcx(usdc.score);

    // get balances after
    BigInteger[] poolAfter = getBalancesIcx(pool);
    BigInteger[] traderAfter = getBalancesIcx(trader.getAddress());

    assertEquals(traderAfter[0], traderBefore[0].add(BigInteger.valueOf(1)));
    assertEquals(traderAfter[1], traderBefore[1].subtract(BigInteger.valueOf(3)));
    assertEquals(poolAfter[0], poolBefore[0].subtract(BigInteger.valueOf(1)));
    assertEquals(poolAfter[1], poolBefore[1].add(BigInteger.valueOf(3)));
  }
}