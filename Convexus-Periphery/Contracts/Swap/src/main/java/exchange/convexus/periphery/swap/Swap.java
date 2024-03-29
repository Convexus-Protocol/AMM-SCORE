/*
 * Copyright 2022 Convexus Protocol
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package exchange.convexus.periphery.swap;


import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import exchange.convexus.utils.BytesUtils;
import exchange.convexus.utils.ICX;
import exchange.convexus.utils.JSONUtils;
import exchange.convexus.utils.ReentrancyLock;
import exchange.convexus.utils.StringUtils;
import static exchange.convexus.utils.TimeUtils.now;

import exchange.convexus.interfaces.irc2.IIRC2ICX;
import exchange.convexus.periphery.interfaces.callback.IConvexusLiquidityManagement;
import exchange.convexus.periphery.interfaces.callback.IConvexusLiquidityManagementAddLiquidity;
import exchange.convexus.periphery.liquidity.AddLiquidityParams;
import exchange.convexus.periphery.liquidity.AddLiquidityResult;
import exchange.convexus.periphery.liquidity.ConvexusLiquidityManagement;
import exchange.convexus.periphery.router.ExactInputParams;
import exchange.convexus.periphery.router.ExactInputSingleParams;
import exchange.convexus.periphery.router.ExactOutputParams;
import exchange.convexus.periphery.router.ExactOutputSingleParams;
import com.eclipsesource.json.JsonObject;

/**
 * @title Swap contract implementation
 * @notice An example contract using the Convexus swap function
 */
public class Swap 
  implements IConvexusLiquidityManagement,
         IConvexusLiquidityManagementAddLiquidity
{
  // ================================================
  // Consts
  // ================================================
  // Contract class name
  private static final String NAME = "Swap";

  // Contract name
  private final String name;
  private final Address swapRouter;

  // Liquidity Manager
  private final ConvexusLiquidityManagement liquidityMgr;

  // For this example, we will set the pool fee to 0.3%.
  public final int poolFee = 3000;

  // This example swaps tokenIn/tokenOut for single path swaps and tokenIn/tokenInOut/tokenOut for multi path swaps.
  public final Address tokenIn;
  public final Address tokenOut;
  public final Address tokenInOut;

  // ================================================
  // DB Variables
  // ================================================
  private final ReentrancyLock reentreancy = new ReentrancyLock(NAME + "_reentreancy");

  // ================================================
  // Methods
  // ================================================
  /**
   *  Contract constructor
   */
  public Swap (
    Address swapRouter,
    Address factory,
    Address tokenIn,
    Address tokenOut,
    Address tokenInOut
  ) {
    this.name = "Convexus " + IIRC2ICX.symbol(tokenIn) + "-" + IIRC2ICX.symbol(tokenInOut) + "-" + IIRC2ICX.symbol(tokenOut) + " Swap";
    this.swapRouter = swapRouter;
    this.tokenIn = tokenIn;
    this.tokenOut = tokenOut;
    this.tokenInOut = tokenInOut;
    
    this.liquidityMgr = new ConvexusLiquidityManagement(factory);
  }

  /**
   * @notice swapExactInputSingle swaps a fixed amount of `tokenIn` for a maximum possible amount of `tokenOut`
   * using the `tokenIn`/`tokenOut` 0.3% pool by calling `exactInputSingle` in the swap router.
   * @dev The calling address must approve this contract to spend at least `amountIn` worth of its `tokenIn` for this function to succeed.
   * @param amountIn The exact amount of `tokenIn` that will be swapped for `tokenOut`.
   * @return amountOut The amount of `tokenOut` received.
   */
  // @External - this method is external through tokenFallback
  private void swapExactInputSingle (Address caller, Address tokenIn, BigInteger amountIn) {
    reentreancy.lock(true);

    // Naively set amountOutMinimum to 0. In production, use an oracle or other data source to choose a safer value for amountOutMinimum.
    // We also set the sqrtPriceLimitx96 to be 0 to ensure we swap our exact input amount.
    ExactInputSingleParams params = new ExactInputSingleParams(
      tokenOut,
      poolFee,
      caller,
      now(),
      ZERO,
      ZERO
    );

    // The call to `exactInputSingle` executes the swap.
    // Forward tokenIn to the router and call the "exactInputSingle" method
    IIRC2ICX.transfer(tokenIn, this.swapRouter, amountIn, "exactInputSingle", params);

    reentreancy.lock(false);
  }

  /**
   * @notice swapExactOutputSingle swaps a minimum possible amount of `tokenIn` for a fixed amount of `tokenOut`.
   * @param amountOut The exact amount of `tokenOut` to receive from the swap.
   * @param amountInMaximum The amount of `tokenIn` we are willing to spend to receive the specified amount of `tokenOut`.
   * @return amountIn The amount of `tokenIn` actually spent in the swap.
   */
  // @External - this method is external through tokenFallback
  private void swapExactOutputSingle(
    Address caller, 
    Address tokenIn, 
    BigInteger amountOut,
    BigInteger amountInMaximum
  ) {
    reentreancy.lock(true);

    // Approve the router to spend the specifed `amountInMaximum` of DAI.
    // In production, you should choose the maximum amount to spend based on oracles or other data sources to achieve a better swap.
    ExactOutputSingleParams params = new ExactOutputSingleParams(
      this.tokenOut,
      poolFee,
      caller,
      now(),
      amountOut,
      ZERO
    );

    // Executes the swap returning the token exceess not needed to spend to receive the desired amountOut.
    // Forward `tokenIn` to the router and call the "exactOutputSingle" method
    IIRC2ICX.transfer(tokenIn, this.swapRouter, amountInMaximum, "exactOutputSingle", params);

    BigInteger excess = IIRC2ICX.balanceOf(tokenIn, Context.getAddress());
    // send back the tokens excess to the caller if there's any
    if (excess.compareTo(ZERO) > 0) {
      IIRC2ICX.transfer(tokenIn, caller, excess, "excess");
    }

    reentreancy.lock(false);
  }

  // @External - this method is external through tokenFallback
  private void swapExactInputMultihop(Address caller, Address tokenIn, BigInteger amountIn) {
    reentreancy.lock(true);

    // Multiple pool swaps are encoded through bytes called a `path`. A path is a sequence of token addresses and poolFees that define the pools used in the swaps.
    // The format for pool encoding is (tokenIn, fee, tokenOut/tokenIn, fee, tokenOut) where tokenIn/tokenOut parameter is the shared token across the pools.
    // Since we are swapping `tokenIn` to `tokenInOut` and then `tokenInOut` to `tokenOut` the path encoding is (`tokenIn`, 0.3%, `tokenInOut`, 0.3%, `tokenOut`).
    ExactInputParams params = new ExactInputParams(
      BytesUtils.concat(
        tokenIn.toByteArray(),
        BytesUtils.intToBytes(poolFee),
        tokenInOut.toByteArray(),
        BytesUtils.intToBytes(poolFee),
        tokenOut.toByteArray()
      ),
      caller,
      now(),
      ZERO
    );

    // Executes the swap.
    // Forward tokenIn to the router and call the "exactInput" method
    IIRC2ICX.transfer(tokenIn, this.swapRouter, amountIn, "exactInput", params);

    reentreancy.lock(false);
  }
  
  /**
   * @notice swapExactOutputMultihop swaps a minimum possible amount of `tokenIn` for a fixed amount of WETH through an intermediary pool.
   * For this example, we want to swap `tokenIn` for `tokenOut` through a `tokenInOut` pool but we specify the desired amountOut of `tokenOut`. Notice how the path encoding is slightly different in for exact output swaps.
   * @dev The calling address must approve this contract to spend its `tokenIn` for this function to succeed. As the amount of input `tokenIn` is variable,
   * the calling address will need to approve for a slightly higher amount, anticipating some variance.
   * @param amountOut The desired amount of `tokenOut`.
   * @param amountInMaximum The maximum amount of `tokenIn` willing to be swapped for the specified amountOut of `tokenOut`.
   * @return amountIn The amountIn of `tokenIn` actually spent to receive the desired amountOut.
   */
  // @External - this method is external through tokenFallback
  private void swapExactOutputMultihop(
    Address caller, 
    Address tokenIn,
    BigInteger amountOut,
    BigInteger amountInMaximum
  ) {
    reentreancy.lock(true);

    // The parameter path is encoded as (tokenOut, fee, tokenIn/tokenOut, fee, tokenIn)
    // The tokenIn/tokenOut field is the shared token between the two pools used in the multiple pool swap. In this case `tokenInOut` is the "shared" token.
    // For an exactOutput swap, the first swap that occurs is the swap which returns the eventual desired token.
    // In this case, our desired output token is `tokenOut` so that swap happpens first, and is encoded in the path accordingly.
    ExactOutputParams params = new ExactOutputParams(
      BytesUtils.concat(
        tokenOut.toByteArray(),
        BytesUtils.intToBytes(poolFee),
        tokenInOut.toByteArray(),
        BytesUtils.intToBytes(poolFee),
        tokenIn.toByteArray()
      ), 
      caller,
      now(),
      amountOut
    );
    
    // Executes the swap returning the token exceess not needed to spend to receive the desired amountOut.
    // Forward `tokenIn` to the router and call the "exactOutput" method
    IIRC2ICX.transfer(tokenIn, this.swapRouter, amountInMaximum, "exactOutput", params);

    BigInteger excess = IIRC2ICX.balanceOf(tokenIn, Context.getAddress());
    // send back the tokens excess to the caller if there's any
    if (excess.compareTo(ZERO) > 0) {
      IIRC2ICX.transfer(tokenIn, caller, excess, "excess");
    }

    reentreancy.lock(false);
  }
  
  @External
  @Payable
  public void swapExactInputSingleIcx () {
    swapExactInputSingle(Context.getCaller(), ICX.getAddress(), Context.getValue());
  }

  @External
  @Payable
  public void swapExactInputMultihopIcx () {
    swapExactInputMultihop(Context.getCaller(), ICX.getAddress(), Context.getValue());
  }

  @External
  @Payable
  public void swapExactOutputSingleIcx (BigInteger amountOut) {
    swapExactOutputSingle(Context.getCaller(), ICX.getAddress(), amountOut, Context.getValue());
  }

  @External
  @Payable
  public void swapExactOutputMultihopIcx (BigInteger amountOut) {
    swapExactOutputMultihop(Context.getCaller(), ICX.getAddress(), amountOut, Context.getValue());
  }

  @External
  @Payable
  public void depositIcx () {
    this.liquidityMgr.depositIcx();
  }
  
  @External
  public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) throws Exception {
    JsonObject root = JSONUtils.parse(_data);
    String method = root.get("method").asString();
    Address token = Context.getCaller();

    // Ensure we received `tokenIn` as input
    Context.require(token.equals(this.tokenIn));

    switch (method)
    {
      case "swapExactInputSingle": {
        swapExactInputSingle(_from, token, _value);
        break;
      }

      case "swapExactInputMultihop": {
        swapExactInputMultihop(_from, token, _value);
        break;
      }

      case "swapExactOutputSingle": {
        JsonObject params = root.get("params").asObject();
        BigInteger amountOut = StringUtils.toBigInt(params.get("amountOut").asString());
        BigInteger amountInMaximum = _value;
        swapExactOutputSingle(_from, token, amountOut, amountInMaximum);
        break;
      }

      case "swapExactOutputMultihop": {
        JsonObject params = root.get("params").asObject();
        BigInteger amountOut = StringUtils.toBigInt(params.get("amountOut").asString());
        BigInteger amountInMaximum = _value;
        swapExactOutputMultihop(_from, token, amountOut, amountInMaximum);
        break;
      }

      // Implements LiquidityManager
      case "deposit": 
      {
        // Accept the incoming token transfer
        deposit(_from, token, _value);
        break;
      }

      default:
        Context.revert("tokenFallback: Unimplemented tokenFallback action");
    }
  }

  // ================================================
  // Implements LiquidityManager
  // ================================================
  /**
   * @notice Called to `Context.getCaller()` after minting liquidity to a position from ConvexusPool#mint.
   * @dev In the implementation you must pay the pool tokens owed for the minted liquidity.
   * The caller of this method must be checked to be a ConvexusPool deployed by the canonical ConvexusFactory.
   * @param amount0Owed The amount of token0 due to the pool for the minted liquidity
   * @param amount1Owed The amount of token1 due to the pool for the minted liquidity
   * @param data Any data passed through by the caller via the mint call
   */
  @External
  public void convexusMintCallback (
    BigInteger amount0Owed,
    BigInteger amount1Owed,
    byte[] data
  ) {
    this.liquidityMgr.convexusMintCallback(amount0Owed, amount1Owed, data);
  }

  /**
   * @notice Add liquidity to an initialized pool
   * @dev Liquidity must have been provided beforehand
   */
  @External
  public AddLiquidityResult addLiquidity (AddLiquidityParams params) {
    return this.liquidityMgr.addLiquidity(params);
  }

  /**
   * @notice Remove funds from the liquidity manager
   */
  @External
  public void withdraw (Address token) {
    this.liquidityMgr.withdraw(token);
  }

  /**
   * @notice Remove all funds from the liquidity manager previously deposited by `Context.getCaller`
   */
  @External
  public void withdraw_all () {
    this.liquidityMgr.withdraw_all();
  }

  // @External - this method is external through tokenFallback
  public void deposit(Address caller, Address tokenIn, BigInteger amountIn) {
    this.liquidityMgr.deposit(caller, tokenIn, amountIn);
  }

  @External(readonly = true)
  public BigInteger deposited(Address user, Address token) {
    return this.liquidityMgr.deposited(user, token);
  }

  @External(readonly = true)
  public int depositedTokensSize(Address user) {
    return this.liquidityMgr.depositedTokensSize(user);
  }

  @External(readonly = true)
  public Address depositedToken(Address user, int index) {
    return this.liquidityMgr.depositedToken(user, index);
  }

  // ================================================
  // Public variable getters
  // ================================================
  @External(readonly = true)
  public String name() {
    return this.name;
  }
}
