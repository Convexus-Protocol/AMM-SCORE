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

package exchange.convexus.router;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import exchange.convexus.factory.ConvexusFactoryMock;
import exchange.convexus.initializer.ConvexusPoolInitializerUtils;
import exchange.convexus.librairies.Path;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.nft.NFTUtils;
import exchange.convexus.positiondescriptor.NonfungibleTokenPositionDescriptor;
import exchange.convexus.positionmgr.NonFungiblePositionManager;
import exchange.convexus.testtokens.Baln;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;
import exchange.convexus.utils.AddressUtils;
import exchange.convexus.utils.ArrayUtils;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.ConvexusTest;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.ScoreSpy;
import exchange.convexus.utils.TimeUtils;
import score.Address;

public class SwapRouterTest extends ConvexusTest {

  ScoreSpy<SwapRouter> router;
  ScoreSpy<ConvexusFactoryMock> factory;
  ScoreSpy<NonfungibleTokenPositionDescriptor> positiondescriptor;
  ScoreSpy<NonFungiblePositionManager> nft;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<Baln> baln;
  protected final Account trader = sm.createAccount();

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  BigInteger liquidity = BigInteger.valueOf(1000000);

  void setup_tokens () throws Exception {
    sicx = deploy_sicx();
    usdc = deploy_usdc();
    baln = deploy_baln();
    
    // Transfer some funds to Alice
    sicx.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    baln.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    // Transfer some funds to trader
    sicx.invoke(owner, "mintTo", trader.getAddress(), BigInteger.valueOf(10));
    usdc.invoke(owner, "mintTo", trader.getAddress(), BigInteger.valueOf(10));
    baln.invoke(owner, "mintTo", trader.getAddress(), BigInteger.valueOf(10));
  }

  void setup_router () throws Exception {
    factory = deploy_factory();
    router = deploy_router(factory.getAddress());
  }

  void setup_nft (Address factoryAddress) throws Exception {
    positiondescriptor = deploy_positiondescriptor();
    nft = deploy_nft(factoryAddress, positiondescriptor.getAddress());
  }
  
  void createPool (Class<?> poolClass, Score token0, Score token1) {
    if (AddressUtils.compareTo(token0.getAddress(), token1.getAddress()) > 0) {
      Score tmp = token0;
      token0 = token1;
      token0 = tmp;
    }

    int fee = FEE_AMOUNTS[MEDIUM];
    int tickSpacing = TICK_SPACINGS[MEDIUM];
    int tickLower = getMinTick(tickSpacing);
    int tickUpper = getMaxTick(tickSpacing);
    
    ConvexusPoolInitializerUtils.createAndInitializePoolIfNecessary(poolClass, alice, factory, token0.getAddress(), token1.getAddress(), fee, encodePriceSqrt(1, 1), tickSpacing);
    
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), token0, BigInteger.valueOf(1000000));
    ConvexusLiquidityUtils.deposit(alice, nft.getAddress(), token1, BigInteger.valueOf(1000000));

    NFTUtils.mint(
      nft, 
      alice, 
      token0.getAddress(), token1.getAddress(), 
      fee, 
      tickLower, tickUpper, 
      BigInteger.valueOf(1000000), 
      BigInteger.valueOf(1000000), 
      ZERO, BigInteger.ZERO, 
      alice.getAddress(), 
      TimeUtils.now().add(ONE)
    );
  }

  void exactInput (Score token0, Score token1) {
    exactInput(new Score[] {token0, token1}, BigInteger.valueOf(3), BigInteger.ONE);
  }

  void exactInput (Score token0, Score token1, Score token2, BigInteger amountIn, BigInteger amountOutMinimum) {
    exactInput(new Score[] {token0, token1, token2}, amountIn, amountOutMinimum);
  }

  void exactInput (Score[] tokensScore, BigInteger amountIn, BigInteger amountOutMinimum) {

    Address[] tokens = new Address[tokensScore.length];
    for (int i = 0; i < tokens.length; i++) {
      tokens[i] = tokensScore[i].getAddress();
    }

    // ensure that the swap fails if the limit is any tighter
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      SwapRouterUtils.exactInput(
        trader, 
        tokensScore[0], 
        router.getAddress(), 
        amountIn,
        Path.encodePath(tokens, ArrayUtils.newFill(tokens.length - 1, FEE_AMOUNTS[MEDIUM])), 
        trader.getAddress(), 
        TimeUtils.now().add(ONE), 
        amountOutMinimum.add(ONE)
      ),
      "exactInput: Too little received"
    );

    SwapRouterUtils.exactInput(
      trader, 
      tokensScore[0], 
      router.getAddress(), 
      amountIn,
      Path.encodePath(tokens, ArrayUtils.newFill(tokens.length - 1, FEE_AMOUNTS[MEDIUM])), 
      trader.getAddress(), 
      TimeUtils.now().add(ONE), 
      amountOutMinimum
    );
  }

  void exactInputSingle (Score tokenIn, Score tokenOut) {
    exactInputSingle (tokenIn, tokenOut, BigInteger.valueOf(3), ONE);
  }

  void exactInputSingle (Score tokenIn, Score tokenOut, BigInteger amountIn, BigInteger amountOutMinimum) {
    // ensure that the swap fails if the limit is any tighter
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      SwapRouterUtils.exactInputSingle(
        trader, 
        tokenIn, 
        router.getAddress(), 
        amountIn,
        tokenOut.getAddress(),
        FEE_AMOUNTS[MEDIUM],
        trader.getAddress(),
        TimeUtils.now().add(ONE),
        amountOutMinimum.add(ONE),
        AddressUtils.compareTo(tokenIn.getAddress(), tokenOut.getAddress()) < 0 
        ? new BigInteger("4295128740")
        : new BigInteger("1461446703485210103287273052203988822378723970341")
      ),
      "exactInputSingle: Too little received"
    );

    SwapRouterUtils.exactInputSingle(
      trader, 
      tokenIn, 
      router.getAddress(), 
      amountIn,
      tokenOut.getAddress(),
      FEE_AMOUNTS[MEDIUM],
      trader.getAddress(),
      TimeUtils.now().add(ONE),
      amountOutMinimum,
      AddressUtils.compareTo(tokenIn.getAddress(), tokenOut.getAddress()) < 0 
      ? new BigInteger("4295128740")
      : new BigInteger("1461446703485210103287273052203988822378723970341")
    );
  }

  
  void exactOutputSingle (Score tokenIn, Score tokenOut) {
    exactOutputSingle (tokenIn, tokenOut, ONE, BigInteger.valueOf(3));
  }

  void exactOutputSingle (Score tokenIn, Score tokenOut, BigInteger amountOut, BigInteger amountInMaximum) {
    // ensure that the swap fails if the limit is any tighter
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      SwapRouterUtils.exactOutputSingle(
        trader, 
        tokenIn, 
        router.getAddress(), 
        amountInMaximum.subtract(ONE),
        tokenOut.getAddress(),
        FEE_AMOUNTS[MEDIUM],
        trader.getAddress(),
        TimeUtils.now().add(ONE),
        amountOut,
        AddressUtils.compareTo(tokenIn.getAddress(), tokenOut.getAddress()) < 0 
          ? new BigInteger("4295128740")
          : new BigInteger("1461446703485210103287273052203988822378723970341")
      ),
      "Insufficient balance"
    );

    SwapRouterUtils.exactOutputSingle(
      trader, 
      tokenIn, 
      router.getAddress(), 
      amountInMaximum,
      tokenOut.getAddress(),
      FEE_AMOUNTS[MEDIUM],
      trader.getAddress(),
      TimeUtils.now().add(ONE),
      amountOut,
      AddressUtils.compareTo(tokenIn.getAddress(), tokenOut.getAddress()) < 0 
        ? new BigInteger("4295128740")
        : new BigInteger("1461446703485210103287273052203988822378723970341")
    );
  }


  void exactOutput (Score token0, Score token1) {
    exactOutput(new Score[] {token0, token1}, ONE, BigInteger.valueOf(3));
  }

  void exactOutput (Score token0, Score token1, Score token2, BigInteger amountOut, BigInteger amountInMaximum) {
    exactOutput(new Score[] {token0, token1, token2}, amountOut, amountInMaximum);
  }

  public Address[] reverse(Address[] array) {
    Address[] result = new Address[array.length];
    System.arraycopy(array, 0, result, 0, array.length);
    List<Address> list = Arrays.asList(result);
    Collections.reverse(list);
    return list.toArray(result);
  }

  void exactOutput (Score[] tokensScore, BigInteger amountOut, BigInteger amountInMaximum) {

    Address[] tokens = new Address[tokensScore.length];
    for (int i = 0; i < tokens.length; i++) {
      tokens[i] = tokensScore[i].getAddress();
    }

    // ensure that the swap fails if the limit is any tighter
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      SwapRouterUtils.exactOutput(
        trader, 
        tokensScore[0], 
        router.getAddress(), 
        amountInMaximum.subtract(ONE),
        Path.encodePath(reverse(tokens), ArrayUtils.newFill(tokens.length - 1, FEE_AMOUNTS[MEDIUM])), 
        trader.getAddress(), 
        TimeUtils.now().add(ONE), 
        amountOut
      ),
      "Insufficient balance"
    );

    SwapRouterUtils.exactOutput(
      trader, 
      tokensScore[0], 
      router.getAddress(), 
      amountInMaximum,
      Path.encodePath(reverse(tokens), ArrayUtils.newFill(tokens.length - 1, FEE_AMOUNTS[MEDIUM])), 
      trader.getAddress(), 
      TimeUtils.now().add(ONE), 
      amountOut
    );
  }

  BigInteger[] getBalances (Address who) {
    return new BigInteger[] {
      (BigInteger) sicx.call("balanceOf", who),
      (BigInteger) usdc.call("balanceOf", who),
      (BigInteger) baln.call("balanceOf", who)
    };
  }

}
