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

package exchange.convexus.periphery.router;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.periphery.interfaces.callback.IConvexusLiquidityManagement;
import exchange.convexus.periphery.librairies.CallbackValidation;
import exchange.convexus.periphery.librairies.Path;
import exchange.convexus.periphery.librairies.PeripheryPayments;
import exchange.convexus.periphery.librairies.PoolAddressLib;
import exchange.convexus.periphery.liquidity.ConvexusLiquidityManagement;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.PairAmounts;
import exchange.convexus.pool.PoolData;
import exchange.convexus.interfaces.irc2.IIRC2ICX;
import exchange.convexus.utils.AddressUtils;
import exchange.convexus.utils.BytesUtils;
import exchange.convexus.utils.ICX;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.ReentrancyLock;
import exchange.convexus.utils.TimeUtils;
import score.Address;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import scorex.io.Reader;
import scorex.io.StringReader;

/**
 * @title Convexus Swap Router
 * @notice Router for stateless execution of swaps against Convexus
 */
public class SwapRouter 
  implements IConvexusLiquidityManagement
{
  // ================================================
  // Consts
  // ================================================
  
  // Contract class name
  private static final String NAME = "SwapRouter";

  // Contract name
  private final String name;

  /// @dev Used as the placeholder value for amountInCached, because the computed amount in for an exact output swap
  /// can never actually be this value
  private BigInteger DEFAULT_AMOUNT_IN_CACHED = IntUtils.MAX_UINT256;

  // address of the Convexus factory
  public final Address factory;

  // Liquidity Manager
  private final ConvexusLiquidityManagement liquidityMgr;

  // ================================================
  // DB Variables
  // ================================================
  private final VarDB<BigInteger> amountInCached = Context.newVarDB(NAME + "_amountInCached", BigInteger.class);
  private final ReentrancyLock reentreancy = new ReentrancyLock(NAME + "_reentreancy");

  // ================================================
  // Event Logs
  // ================================================

  // ================================================
  // Methods
  // ================================================
  /**
   *  @notice Contract constructor
   */
  public SwapRouter(Address factory) {
    this.name = "Convexus Swap Router";
    this.factory = factory;
    this.liquidityMgr = new ConvexusLiquidityManagement(factory);
  }

  /**
   * @dev Returns the pool for the given token pair and fee. The pool contract may or may not exist.
   */
  private Address getPool (
    Address tokenA,
    Address tokenB,
    int fee
  ) {
    return PoolAddressLib.getPool(factory, PoolAddressLib.getPoolKey(tokenA, tokenB, fee));
  }

  /**
   * Called to `msg.sender` after executing a swap via `ConvexusPool::swap`.
   * @dev In the implementation you must pay the pool tokens owed for the swap. The caller of this method must be checked to be a ConvexusPool deployed by the canonical ConvexusFactory. amount0Delta and amount1Delta can both be 0 if no tokens were swapped.
   * @param amount0Delta The amount of token0 that was sent (negative) or must be received (positive) by the pool by the end of the swap. If positive, the callback must send that amount of token0 to the pool.
   * @param amount1Delta The amount of token1 that was sent (negative) or must be received (positive) by the pool by the end of the swap. If positive, the callback must send that amount of token1 to the pool.
   * @param data Any data passed through by the caller via the swap call
   */
  @External
  public void convexusSwapCallback (
    BigInteger amount0Delta,
    BigInteger amount1Delta,
    byte[] data
  ) {
    Context.require(
      amount0Delta.compareTo(ZERO) > 0 
    ||  amount1Delta.compareTo(ZERO) > 0, 
      "convexusSwapCallback: swaps entirely within 0-liquidity regions are not supported");

    final Address caller = Context.getCaller();

    ObjectReader dataReader = Context.newByteArrayObjectReader("RLPn", data);
    SwapCallbackData callbackData = SwapCallbackData.readObject(dataReader);
    PoolData pool = Path.decodeFirstPool(callbackData.path);
    Address tokenIn = pool.tokenA;
    Address tokenOut = pool.tokenB;
    int fee = pool.fee;

    CallbackValidation.verifyCallback(this.factory, tokenIn, tokenOut, fee);

    boolean isExactInput;
    BigInteger amountToPay;
    if (amount0Delta.compareTo(ZERO) > 0) {
      isExactInput = AddressUtils.compareTo(tokenIn, tokenOut) < 0;
      amountToPay = amount0Delta;
    } else {
      isExactInput = AddressUtils.compareTo(tokenOut, tokenIn) < 0;
      amountToPay = amount1Delta;
    }

    if (isExactInput) {
      PeripheryPayments.pay(tokenIn, caller, amountToPay);
    } else {
      // either initiate the next swap or pay
      if (Path.hasMultiplePools(callbackData.path)) {
        callbackData.path = Path.skipToken(callbackData.path);
        exactOutputInternal(amountToPay, caller, ZERO, callbackData);
      } else {
        this.amountInCached.set(amountToPay);
        tokenIn = tokenOut; // swap in/out because exact output swaps are reversed
        PeripheryPayments.pay(tokenIn, caller, amountToPay);
      }
    }
  }

  /**
   * @notice Swaps `amountIn` of one token for as much as possible of another token
   * 
   * Access: Everyone
   * 
   * @param caller The method caller. This field is handled by tokenFallback
   * @param tokenIn The tokenIn address. This field is handled by tokenFallback
   * @param amountIn The token amount sent. This field is handled by tokenFallback
   * @param params The parameters necessary for the swap, encoded as `ExactInputSingleParams`
   */
  // @External - this method is external through tokenFallback
  private void exactInputSingle (
    Address caller, 
    Address tokenIn, 
    BigInteger amountIn, 
    ExactInputSingleParams params
  ) {
    reentreancy.lock(true);
    this.checkDeadline(params.deadline);

    BigInteger amountOut = exactInputInternal(
      amountIn, 
      params.recipient, 
      params.sqrtPriceLimitX96, 
      new SwapCallbackData(
        BytesUtils.concat(
          tokenIn.toByteArray(),
          BytesUtils.intToBytes(params.fee),
          params.tokenOut.toByteArray()
        ), 
        caller
      )
    );

    Context.require(amountOut.compareTo(params.amountOutMinimum) >= 0,
      "exactInputSingle: Too little received");

    reentreancy.lock(false);
  }

  /**
   * @notice Swaps as little as possible of one token for `amountOut` of another token
   * 
   * Access: Everyone
   * 
   * @param caller: The method caller. This field is handled by tokenFallback
   * @param tokenIn: The tokenIn address. This field is handled by tokenFallback
   * @param amountInMaximum: The maximum amount of `token0` willing to be swapped for the specified amountOut of `token1`. This field is handled by tokenFallback.
   * @param params The parameters necessary for the swap, encoded as `ExactOutputSingleParams`
   */
  // @External - this method is external through tokenFallback
  private void exactOutputSingle (
    Address caller,
    Address tokenIn,
    BigInteger amountInMaximum,
    ExactOutputSingleParams params
  ) {
    reentreancy.lock(true);
    this.checkDeadline(params.deadline);

    // avoid an db loading by using the swap return data
    BigInteger amountIn = exactOutputInternal(
      params.amountOut, 
      params.recipient, 
      params.sqrtPriceLimitX96, 
      new SwapCallbackData(
        BytesUtils.concat(
          params.tokenOut.toByteArray(), 
          BytesUtils.intToBytes(params.fee), 
          tokenIn.toByteArray()
        ), 
        caller // caller pays for the first hop
      )
    );

    Context.require(amountIn.compareTo(amountInMaximum) <= 0, 
      "exactOutputSingle: Too much requested");

    // has to be reset even though we don't use it in the single hop case
    this.amountInCached.set(DEFAULT_AMOUNT_IN_CACHED);

    // send back the tokens excess to the caller if there's any
    BigInteger excess = amountInMaximum.subtract(amountIn);
    if (excess.compareTo(ZERO) > 0) {
      IIRC2ICX.transfer(tokenIn, caller, excess, "excess");
    }

    reentreancy.lock(false);
  }

  /**
   * @notice Swaps `amountIn` of one token for as much as possible of another along the specified path
   * 
   * Access: Everyone
   * 
   * @param caller: The method caller. This field is handled by tokenFallback
   * @param tokenIn: The tokenIn address. This field is handled by tokenFallback
   * @param amountIn The token amount sent. This field is handled by tokenFallback
   * @param params The parameters necessary for the multi-hop swap, encoded as `ExactInputParams`
   */
  // @External - this method is external through tokenFallback
  private void exactInput (
    Address caller,
    Address tokenIn,
    BigInteger amountIn,
    ExactInputParams params
  ) {
    reentreancy.lock(true);
    this.checkDeadline(params.deadline);

    // Check if first token matches with the path
    PoolData pool = Path.decodeFirstPool(params.path);

    Context.require(pool.tokenA.equals(tokenIn), 
      "exactInput: Path doesn't match with the token sent");

    BigInteger amountOut = ZERO;
    Address payer = caller; // caller pays for the first hop

    while (true) {
      boolean hasMultiplePools = Path.hasMultiplePools(params.path);

      // the outputs of prior swaps become the inputs to subsequent ones
      amountIn = exactInputInternal(
        amountIn, 
        hasMultiplePools ? Context.getAddress() : params.recipient, // for intermediate swaps, this contract custodies
        ZERO, 
        new SwapCallbackData(
          Path.getFirstPool(params.path), // only the first pool in the path is necessary
          payer
        )
      );
      
      // decide whether to continue or terminate
      if (hasMultiplePools) {
        payer = Context.getAddress(); // at this point, the caller has paid
        params.path = Path.skipToken(params.path);
      } else {
        amountOut = amountIn;
        break;
      }
    }

    Context.require(amountOut.compareTo(params.amountOutMinimum) >= 0, 
      "exactInput: Too little received");

    reentreancy.lock(false);
  }

  /**
   * @notice Swaps as little as possible of one token for `amountOut` of another along the specified path (reversed)
   * 
   * Access: Everyone
   * 
   * @param caller: The method caller. This field is handled by tokenFallback
   * @param tokenIn: The tokenIn address. This field is handled by tokenFallback
   * @param amountInMaximum: The maximum amount of `token0` willing to be swapped for the specified amountOut of `token1`. This field is handled by tokenFallback.
   * @param params The parameters necessary for the swap, encoded as `ExactOutputParams`
   */
  // @External - this method is external through tokenFallback
  private void exactOutput (
    Address caller, 
    Address tokenIn, 
    BigInteger amountInMaximum, 
    ExactOutputParams params
  ) {
    reentreancy.lock(true);
    this.checkDeadline(params.deadline);
    
    // Check if last token matches with the path
    PoolData pool = Path.decodeLastPool(params.path);
    Context.require(pool.tokenB.equals(tokenIn), 
      "exactOutput: Path doesn't match with the token sent");

    // it's okay that the payer is fixed to `caller` here, as they're only paying for the "final" exact output
    // swap, which happens first, and subsequent swaps are paid for within nested callback frames
    exactOutputInternal(
      params.amountOut, 
      params.recipient, 
      ZERO, 
      new SwapCallbackData(params.path, caller)
    );

    BigInteger amountIn = this.amountInCached.get();

    Context.require(amountIn.compareTo(amountInMaximum) <= 0, 
      "exactOutput: Too much requested");

    amountInCached.set(DEFAULT_AMOUNT_IN_CACHED);
    reentreancy.lock(false);
  }

  /**
   * @notice Swaps `amountIn` of one token for as much as possible of another token
   * @param params The parameters necessary for the swap, encoded as `ExactInputSingleParams`
   * @return amountOut The amount of the received token
   */
  private BigInteger exactInputInternal(
    BigInteger amountIn,
    Address recipient,
    BigInteger sqrtPriceLimitX96,
    SwapCallbackData data
  ) {
    // allow swapping to the router address with address 0
    if (recipient.equals(AddressUtils.ZERO_ADDRESS)) {
      recipient = Context.getAddress();
    }

    PoolData pool = Path.decodeFirstPool(data.path);
    Address tokenIn = pool.tokenA;
    Address tokenOut = pool.tokenB;
    int fee = pool.fee;

    boolean zeroForOne = AddressUtils.compareTo(tokenIn, tokenOut) < 0;

    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    writer.write(data);

    Address targetPool = getPool(tokenIn, tokenOut, fee);
    Context.require(targetPool != null, 
      "exactInputInternal: Pool doesn't exist");

    PairAmounts result = IConvexusPool.swap(targetPool, 
      recipient,
      zeroForOne,
      amountIn,
      sqrtPriceLimitX96.equals(ZERO)
        ? (zeroForOne ? TickMath.MIN_SQRT_RATIO.add(ONE) : TickMath.MAX_SQRT_RATIO.subtract(ONE))
        : sqrtPriceLimitX96,
      writer.toByteArray()
    );

    return zeroForOne ? result.amount1.negate() : result.amount0.negate();
  }

  /**
   * @notice Swaps as little as possible of one token for `amountOut` of another token
   * @param params The parameters necessary for the swap, encoded as `ExactOutputSingleParams`
   * @return amountIn The amount of the input token
   */
  private BigInteger exactOutputInternal(
    BigInteger amountOut, 
    Address recipient, 
    BigInteger sqrtPriceLimitX96,
    SwapCallbackData data
  ) {
    // allow swapping to the router address with address 0
    if (recipient.equals(AddressUtils.ZERO_ADDRESS)) {
      recipient = Context.getAddress();
    }

    PoolData pool = Path.decodeFirstPool(data.path);
    Address tokenOut = pool.tokenA;
    Address tokenIn = pool.tokenB;
    int fee = pool.fee;

    boolean zeroForOne = AddressUtils.compareTo(tokenIn, tokenOut) < 0;

    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    writer.write(data);

    Address targetPool = getPool(tokenIn, tokenOut, fee);
    Context.require(targetPool != null, 
      "exactInputInternal: Pool doesn't exist");
    
    PairAmounts result = IConvexusPool.swap(targetPool, 
      recipient,
      zeroForOne,
      amountOut.negate(),
      sqrtPriceLimitX96.equals(ZERO)
        ? (zeroForOne ? TickMath.MIN_SQRT_RATIO.add(ONE) : TickMath.MAX_SQRT_RATIO.subtract(ONE))
        : sqrtPriceLimitX96,
      writer.toByteArray()
    );

    BigInteger amount0Delta = result.amount0;
    BigInteger amount1Delta = result.amount1;

    BigInteger amountOutReceived;
    BigInteger amountIn;
    if (zeroForOne) {
      amountIn = amount0Delta;
      amountOutReceived = amount1Delta.negate();
    } else {
      amountIn = amount1Delta;
      amountOutReceived = amount0Delta.negate();
    }
    
    // it's technically possible to not receive the full output amount,
    // so if no price limit has been specified, require this possibility away
    if (sqrtPriceLimitX96.equals(ZERO)) {
      Context.require(amountOutReceived.equals(amountOut),
        "exactOutputInternal: amountOutReceived == amountOut");
    }

    return amountIn;
  }

  @External
  @Payable
  public void exactInputSingleIcx (ExactInputSingleParams params) {
    exactInputSingle(Context.getCaller(), ICX.getAddress(), Context.getValue(), params);
  }

  @External
  @Payable
  public void exactOutputSingleIcx (ExactOutputSingleParams params) {
    exactOutputSingle(Context.getCaller(), ICX.getAddress(), Context.getValue(), params);
  }

  @External
  @Payable
  public void exactInputIcx (ExactInputParams params) {
    exactInput(Context.getCaller(), ICX.getAddress(), Context.getValue(), params);
  }

  @External
  @Payable
  public void exactOutputIcx (ExactOutputParams params) {
    exactOutput(Context.getCaller(), ICX.getAddress(), Context.getValue(), params);
  }

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
      case "exactInputSingle": {
        JsonObject params = root.get("params").asObject();
        exactInputSingle(_from, token, _value, ExactInputSingleParams.fromJson(params));
        break;
      }

      case "exactOutputSingle": {
        JsonObject params = root.get("params").asObject();
        exactOutputSingle(_from, token, _value, ExactOutputSingleParams.fromJson(params));
        break;
      }

      case "exactInput": {
        JsonObject params = root.get("params").asObject();
        exactInput(_from, token, _value, ExactInputParams.fromJson(params));
        break;
      }

      case "exactOutput": {
        JsonObject params = root.get("params").asObject();
        exactOutput(_from, token, _value, ExactOutputParams.fromJson(params));
        break;
      }

      // "deposit" is coming from the Pool
      case "deposit": {
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
   * @notice Remove funds from the liquidity manager
   */
  @External
  public void withdraw (Address token) {
    this.liquidityMgr.withdraw(token);
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
  // Checks
  // ================================================
  /**
   * Check if transaction hasn't reached the deadline
   */
  private void checkDeadline(BigInteger deadline) {
    final BigInteger now = TimeUtils.now();
    Context.require(now.compareTo(deadline) <= 0,
      "checkDeadline: Transaction too old");
  }

  // ================================================
  // Public variable getters
  // ================================================
  @External(readonly = true)
  public String name() {
    return this.name;
  }
}
