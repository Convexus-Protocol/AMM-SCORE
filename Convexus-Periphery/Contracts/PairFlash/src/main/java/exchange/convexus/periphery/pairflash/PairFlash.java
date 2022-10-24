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

package exchange.convexus.periphery.pairflash;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import exchange.convexus.core.interfaces.callback.IConvexusFlashCallback;
import exchange.convexus.interfaces.irc2.IIRC2ICX;
import static exchange.convexus.utils.TimeUtils.now;
import score.Address;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import scorex.io.Reader;
import scorex.io.StringReader;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import exchange.convexus.periphery.interfaces.callback.IConvexusLiquidityManagement;
import exchange.convexus.periphery.interfaces.callback.IConvexusLiquidityManagementAddLiquidity;
import exchange.convexus.periphery.librairies.CallbackValidation;
import exchange.convexus.periphery.librairies.PeripheryPayments;
import exchange.convexus.periphery.librairies.PoolAddressLib;
import exchange.convexus.periphery.liquidity.AddLiquidityParams;
import exchange.convexus.periphery.liquidity.AddLiquidityResult;
import exchange.convexus.periphery.liquidity.ConvexusLiquidityManagement;
import exchange.convexus.periphery.router.ExactInputSingleParams;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.PoolAddress.PoolKey;

/**
 * @title Flash contract implementation
 * @notice An example contract using the Convexus flash function
 */
public class PairFlash 
  implements IConvexusLiquidityManagement,
             IConvexusLiquidityManagementAddLiquidity,
             IConvexusFlashCallback
{
  // ================================================
  // Consts
  // ================================================
  // Contract name
  private final String name;
  // References to other contracts
  private final Address factory;
  private final Address swapRouter;

  // Implement Liquidity Manager
  private final ConvexusLiquidityManagement liquidityMgr;

  // ================================================
  // DB Variables
  // ================================================

  // ================================================
  // Methods
  // ================================================
  /**
   *  Contract constructor
   */
  public PairFlash(
    Address swapRouter,
    Address factory
  ) {
    this.name = "Convexus Pair Flash";
    this.swapRouter = swapRouter;
    this.factory = factory;
    
    this.liquidityMgr = new ConvexusLiquidityManagement(factory);
  }

  private BigInteger routerExactInputSingle (Address tokenIn, BigInteger amountIn, Address tokenOut, BigInteger amountOutMinimum, int poolFee) {
    final Address thisAddress = Context.getAddress();

    BigInteger amountBefore = IIRC2ICX.balanceOf(tokenOut, thisAddress);

    var params = new ExactInputSingleParams(
      tokenOut, 
      poolFee, 
      thisAddress,
      now(), 
      amountOutMinimum,
      ZERO
    );

    // Forward tokenIn to the router and call the "exactInputSingle" method
    // The call to `exactInputSingle` executes the swap.
    IIRC2ICX.transfer(tokenIn, this.swapRouter, amountIn, "exactInputSingle", params);

    BigInteger amountAfter = IIRC2ICX.balanceOf(tokenOut, thisAddress);
    BigInteger amountOut = amountAfter.subtract(amountBefore);

    return amountOut;
  }

  /**
   * @param fee0 The fee from calling flash for token0
   * @param fee1 The fee from calling flash for token1
   * @param data The data needed in the callback passed as FlashCallbackData from `initFlash`
   * @notice implements the callback called from flash
   * @dev fails if the flash is not profitable, meaning the amountOut from the flash is less than the amount borrowed
   */
  @External
  public void convexusFlashCallback (
    BigInteger fee0,
    BigInteger fee1,
    byte[] data
  ) {
    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", data);
    FlashCallbackData decoded = reader.read(FlashCallbackData.class);
    CallbackValidation.verifyCallback(this.factory, decoded.poolKey);

    final Address caller = Context.getCaller();

    Address token0 = decoded.poolKey.token0;
    Address token1 = decoded.poolKey.token1;

    // profitability parameters - we must receive at least the required payment from the arbitrage swaps
    // exactInputSingle will fail if this amount not met
    BigInteger amount0Min = decoded.amount0.add(fee0);
    BigInteger amount1Min = decoded.amount1.add(fee1);

    // call exactInputSingle for swapping token1 for token0 in pool with fee2
    BigInteger amountOut0 = routerExactInputSingle(token1, decoded.amount1, token0, amount0Min, decoded.poolFee2);

    // call exactInputSingle for swapping token0 for token1 in pool with fee3
    BigInteger amountOut1 = routerExactInputSingle(token0, decoded.amount0, token1, amount1Min, decoded.poolFee3);

    // pay the required amounts back to the pair
    if (amount0Min.compareTo(ZERO) > 0) {
      PeripheryPayments.pay(token0, caller, amount0Min);
    }
    if (amount1Min.compareTo(ZERO) > 0) {
      PeripheryPayments.pay(token1, caller, amount1Min);
    }

    // if profitable pay profits to payer
    if (amountOut0.compareTo(amount0Min) > 0) {
      BigInteger profit0 = amountOut0.subtract(amount0Min);
      PeripheryPayments.pay(token0, decoded.payer, profit0);
    }

    if (amountOut1.compareTo(amount1Min) > 0) {
      BigInteger profit1 = amountOut1.subtract(amount1Min);
      PeripheryPayments.pay(token1, decoded.payer, profit1);
    }
  }

  /**
   * @param params The parameters necessary for flash and the callback, passed in as FlashParams
   * @notice Calls the pools flash function with data needed in `convexusFlashCallback`
   */
  @External
  public void initFlash (FlashParams params) {
    PoolKey poolKey = new PoolKey(params.token0, params.token1, params.fee1);
    Address pool = PoolAddressLib.getPool(this.factory, poolKey);

    Context.require(pool != null,
      "initFlash: Pool doesn't exist");

    final Address thisAddress = Context.getAddress();
    final Address caller = Context.getCaller();

    var flashData = new FlashCallbackData(
      params.amount0,
      params.amount1,
      caller,
      poolKey,
      params.fee2,
      params.fee3
    );

    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    writer.write(flashData);

    // recipient of borrowed amounts
    // amount of token0 requested to borrow
    // amount of token1 requested to borrow
    // need amount 0 and amount1 in callback to pay back pool
    // recipient of flash should be THIS contract
    IConvexusPool.flash(pool, thisAddress, params.amount0, params.amount1, writer.toByteArray());
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
   * @notice Accept the incoming ICX transfer
   */
  @External
  @Payable
  public void depositIcx () {
    this.liquidityMgr.depositIcx();
  }

  @External
  public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) throws Exception {
    Reader reader = new StringReader(new String(_data));
    JsonValue input = Json.parse(reader);
    JsonObject root = input.asObject();
    String method = root.get("method").asString();
    Address token = Context.getCaller();

    switch (method)
    {
      case "deposit": 
      {
        // Accept the incoming token transfer
        deposit(_from, token, _value);
        break;
      }

      default:
        Context.revert("tokenFallback: Unimplemented '" + method + "' tokenFallback action");
    }
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
