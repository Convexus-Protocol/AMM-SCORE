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

import static java.math.BigInteger.*;
import java.math.BigInteger;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.mocks.factory.ConvexusFactoryMock;
import exchange.convexus.mocks.pool.ConvexusPoolMock;
import exchange.convexus.periphery.poolreadonly.ConvexusPoolReadOnly;
import exchange.convexus.periphery.positiondescriptor.NonfungibleTokenPositionDescriptor;
import exchange.convexus.periphery.positionmgr.NonFungiblePositionManager;
import exchange.convexus.periphery.quoter.Quoter;
import exchange.convexus.periphery.router.SwapRouter;
import exchange.convexus.test.ConvexusTest;
import exchange.convexus.test.contracts.callee.ConvexusPoolCallee;
import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.ScoreSpy;
import score.Address;
import exchange.convexus.test.tokens.Sicx;
import exchange.convexus.test.tokens.Usdc;

public class PoolReadOnlyTest extends ConvexusTest {

  ScoreSpy<ConvexusFactoryMock> factory;
  ScoreSpy<ConvexusPoolReadOnly> poolReadonly;
  ScoreSpy<Quoter> quoter;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<NonFungiblePositionManager> nft;
  ScoreSpy<SwapRouter> router;
  
  ScoreSpy<ConvexusPoolMock> pool;
  ScoreSpy<ConvexusPoolCallee> callee;

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];

  void setup_router () throws Exception {
    router = deploy_router(factory.getAddress());
  }

  void setup_nft () throws Exception {
    ScoreSpy<NonfungibleTokenPositionDescriptor> positiondescriptor = deploy_positiondescriptor();
    nft = deploy_nft(factory.getAddress(), positiondescriptor.getAddress());
  }

  void setup_tokens () throws Exception {
    sicx = deploy_sicx();
    usdc = deploy_usdc();
    
    // Transfer some funds to Alice
    sicx.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    // Transfer some funds to Bob
    sicx.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
  }

  void setup_poolreadonly () throws Exception {
    factory = deploy_factory();
    poolReadonly = deploy_pool_readonly();
  }
  

  void setup_factory () throws Exception {
    factory = deploy_factory();
  }

  void setup_pool (Address factory, int fee, int tickSpacing) throws Exception {
    pool = deploy_mock_pool(sicx.getAddress(), usdc.getAddress(), factory, fee, tickSpacing);
    callee = deploy_callee();
  }

  protected void initializeAtZeroTick() {
    BigInteger initializeLiquidityAmount = BigInteger.TEN.pow(18).multiply(TWO);
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    int tickSpacing = ((BigInteger) pool.call("tickSpacing")).intValue();
    int min = getMinTick(tickSpacing);
    int max = getMaxTick(tickSpacing);
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("2000000000000000000"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("2000000000000000000"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), min, max, initializeLiquidityAmount);
  }
  
  protected void callSwap (
    Account from, 
    String method, 
    Address pool, 
    Address callee, 
    Score token, 
    BigInteger _value, 
    Address recipient, 
    BigInteger sqrtPriceLimitX96
  ) {
    var params = Json.object()
      .add("pool", pool.toString())
      .add("recipient", recipient.toString())
      .add("sqrtPriceLimitX96", sqrtPriceLimitX96.toString());

    JsonObject data = Json.object()
      .add("method", method)
      .add("params", params);

    byte[] dataBytes = data.toString().getBytes();

    token.invoke(
      from, 
      "transfer", 
      callee, 
      _value, 
      dataBytes
    );
  }

  protected void swap (
    Account from,
    Score inputToken,
    BigInteger amountIn,
    BigInteger amountOut,
    BigInteger sqrtPriceLimitX96
  ) {
    boolean exactInput = amountOut.equals(ZERO);

    String method = 
      inputToken.equals(sicx.score)
        ? exactInput
          ? "swapExact0For1"
          : "swap0ForExact1"
        : exactInput
        ? "swapExact1For0"
        : "swap1ForExact0";
      
    if (sqrtPriceLimitX96 == null) {
      if (inputToken.equals(sicx.score)) {
        sqrtPriceLimitX96 = TickMath.MIN_SQRT_RATIO.add(ONE);
      } else {
        sqrtPriceLimitX96 = TickMath.MAX_SQRT_RATIO.subtract(ONE);
      }
    }

    callSwap (
      from,
      method,
      pool.getAddress(), 
      callee.getAddress(), 
      inputToken, 
      exactInput ? amountIn : amountOut, 
      from.getAddress(), 
      sqrtPriceLimitX96
    );
  }
  
  protected void callSwapIcx (
    Account from, 
    String method, 
    Address pool, 
    ScoreSpy<?> callee, 
    BigInteger value, 
    Address recipient, 
    BigInteger sqrtPriceLimitX96
  ) {
    callee.invoke(from, value, method + "Icx", pool, recipient, sqrtPriceLimitX96);
  }

  protected void swapIcx (
    Account from,
    BigInteger amountIn,
    BigInteger amountOut,
    BigInteger sqrtPriceLimitX96
  ) {
    boolean exactInput = amountOut.equals(ZERO);

    String method = exactInput
          ? "swapExact0For1"
          : "swap0ForExact1";
      
    if (sqrtPriceLimitX96 == null) {
      sqrtPriceLimitX96 = TickMath.MIN_SQRT_RATIO.add(ONE);
    }

    callSwapIcx (
      from,
      method,
      pool.getAddress(), 
      callee, 
      exactInput ? amountIn : amountOut, 
      from.getAddress(), 
      sqrtPriceLimitX96
    );
  }
  
  protected void swapExact0For1 (BigInteger amount, Account caller, BigInteger sqrtPriceLimitX96) {
    swap(caller, sicx.score, amount, ZERO, sqrtPriceLimitX96);
  }
  protected void swapExact0For1 (BigInteger amount, Account caller) {
    swap(caller, sicx.score, amount, ZERO, null);
  }
}
