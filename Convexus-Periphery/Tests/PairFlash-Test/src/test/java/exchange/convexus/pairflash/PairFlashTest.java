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

package exchange.convexus.pairflash;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.mockito.ArgumentCaptor;

import exchange.convexus.factory.ConvexusFactory;
import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.pools.Pool1;
import exchange.convexus.pools.Pool2;
import exchange.convexus.pools.Pool3;
import exchange.convexus.positiondescriptor.NonfungibleTokenPositionDescriptor;
import exchange.convexus.positionmgr.NonFungiblePositionManager;
import exchange.convexus.quoter.QuoteExactInputSingleParams;
import exchange.convexus.quoter.QuoteResult;
import exchange.convexus.quoter.Quoter;
import exchange.convexus.router.SwapRouter;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;
import exchange.convexus.utils.ConvexusTest;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.ScoreSpy;

import static exchange.convexus.nft.NFTUtils.mint;
import static exchange.convexus.utils.TimeUtils.now;
import score.Address;

public class PairFlashTest extends ConvexusTest {

  ScoreSpy<PairFlash> flash;
  ScoreSpy<ConvexusFactory> factory;
  ScoreSpy<SwapRouter> router;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<Pool1> pool1;
  ScoreSpy<Pool2> pool2;
  ScoreSpy<Pool3> pool3;
  ScoreSpy<Quoter> quoter;
  ScoreSpy<NonFungiblePositionManager> nft;
  ScoreSpy<NonfungibleTokenPositionDescriptor> positiondescriptor;
  
  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;

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

  void setup_positionmgr () throws Exception {
    positiondescriptor = deploy_positiondescriptor();
    nft = deploy_nft(factory.getAddress(), positiondescriptor.getAddress());
  }
  
  void setup_pairflash () throws Exception {
    factory = deploy_factory();
    quoter = deploy_quoter(factory.getAddress());
    router = deploy_router(factory.getAddress());
    flash = deploy_flash(router.getAddress(), factory.getAddress());
  }

  <T> ScoreSpy<T> setup_pool (int fee, int tickSpacing, BigInteger sqrtPriceLimitX96, Class<?> poolClass) throws Exception {
    ScoreSpy<T> pool = deploy(poolClass, sicx.getAddress(), usdc.getAddress(), factory.getAddress(), fee, tickSpacing);
    pool.invoke(alice, "initialize", sqrtPriceLimitX96);
    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), fee, pool.getAddress());
    
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), sicx.score, BigInteger.valueOf(1000000));
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), usdc.score, BigInteger.valueOf(1000000));

    mint (
      nft,
      alice, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      fee, 
      getMinTick(tickSpacing),
      getMaxTick(tickSpacing),
      BigInteger.valueOf(1000000),
      BigInteger.valueOf(1000000),
      ZERO, 
      ZERO, 
      alice.getAddress(),
      now().add(ONE)
    );

    return pool;
  }

  void setup_pool1 () throws Exception {
    pool1 = setup_pool(FEE_AMOUNTS[LOW], TICK_SPACINGS[LOW], encodePriceSqrt(5, 10), Pool1.class);
  }

  void setup_pool2 () throws Exception {
    pool2 = setup_pool(FEE_AMOUNTS[MEDIUM], TICK_SPACINGS[MEDIUM], encodePriceSqrt(1, 1), Pool2.class);
  }

  void setup_pool3 () throws Exception {
    pool3 = setup_pool(FEE_AMOUNTS[HIGH], TICK_SPACINGS[HIGH], encodePriceSqrt(20, 10), Pool3.class);
  }

  protected QuoteResult quoteExactInputSingle (
    Account caller,
    Address tokenIn,
    Address tokenOut,
    BigInteger amountIn,
    int fee,
    BigInteger sqrtPriceLimitX96
  ) {
    reset(quoter.spy);
    quoter.invoke(alice, "quoteExactInputSingle", new QuoteExactInputSingleParams(tokenIn, tokenOut, amountIn, fee, sqrtPriceLimitX96));

    ArgumentCaptor<BigInteger> amount = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> sqrtPriceX96After = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<Integer> initializedTicksCrossed = ArgumentCaptor.forClass(Integer.class);
    verify(quoter.spy).QuoteResult(amount.capture(), sqrtPriceX96After.capture(), initializedTicksCrossed.capture());

    return new QuoteResult(amount.getValue(), sqrtPriceX96After.getValue(), initializedTicksCrossed.getValue());
  }

  protected void initFlash (
    Account caller,
    Address token0,
    Address token1,
    int fee1,
    BigInteger amount0,
    BigInteger amount1,
    int fee2,
    int fee3
  ) {
    flash.invoke(caller, "initFlash", new FlashParams(
      token0,
      token1,
      fee1,
      amount0,
      amount1,
      fee2,
      fee3
    ));
  }
}
