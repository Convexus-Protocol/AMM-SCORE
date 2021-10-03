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

package exchange.switchy.router;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import exchange.switchy.librairies.Path;
import exchange.switchy.librairies.PeripheryPayments;
import exchange.switchy.librairies.PoolAddress;
import exchange.switchy.librairies.PoolData;
import exchange.switchy.librairies.TickMath;
import exchange.switchy.librairies.CallbackValidation;
import exchange.switchy.librairies.PairAmounts;
import exchange.switchy.utils.AddressUtils;
import exchange.switchy.utils.ByteReader;
import exchange.switchy.utils.BytesUtils;
import exchange.switchy.utils.IntConstants;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;
import scorex.io.Reader;
import scorex.io.StringReader;

import static exchange.switchy.librairies.BlockTimestamp._blockTimestamp;

/**
 * @title Switchy Swap Router
 * @notice Router for stateless execution of swaps against Switchy
 */
public class SwapRouter {
 
    // ================================================
    // Consts
    // ================================================
    
    // Contract class name
    private static final String NAME = "SwapRouter";

    // Contract name
    private final String name;

    /// @dev Used as the placeholder value for amountInCached, because the computed amount in for an exact output swap
    /// can never actually be this value
    private BigInteger DEFAULT_AMOUNT_IN_CACHED = IntConstants.MAX_UINT256;

    // address of the Switchy factory
    public final Address factory;
    // address of wICX
    public final Address wICX;

    // ================================================
    // DB Variables
    // ================================================
    private final VarDB<BigInteger> amountInCached = Context.newVarDB(NAME + "_amountInCached", BigInteger.class);

    // ================================================
    // Methods
    // ================================================
    /**
     *  @notice Contract constructor
     */
    public SwapRouter(
        Address _factory,
        Address _wICX
    ) {
        this.name = "Switchy Swap Router";
        this.factory = _factory;
        this.wICX = _wICX;
    }

    /**
     * @dev Returns the pool for the given token pair and fee. The pool contract may or may not exist.
     */
    private Address getPool (
        Address tokenA,
        Address tokenB,
        int fee
    ) {
        return PoolAddress.computeAddress(factory, PoolAddress.getPoolKey(tokenA, tokenB, fee));
    }

    @External
    public void switchySwapCallback (
        BigInteger amount0Delta,
        BigInteger amount1Delta,
        byte[] data
    ) {
        Context.require(
            amount0Delta.compareTo(ZERO) > 0 
        ||  amount1Delta.compareTo(ZERO) > 0, 
            "switchySwapCallback: swaps entirely within 0-liquidity regions are not supported");

        final Address caller = Context.getCaller();
        SwapCallbackData callbackData = SwapCallbackData.fromBytes(new ByteReader(data));
        PoolData pool = Path.decodeFirstPool(new ByteReader(callbackData.path));
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
            PeripheryPayments.pay(this.wICX, tokenIn, callbackData.payer, caller, amountToPay);
        } else {
            // either initiate the next swap or pay
            if (Path.hasMultiplePools(callbackData.path)) {
                callbackData.path = Path.skipToken(callbackData.path);
                exactOutputInternal(amountToPay, caller, ZERO, callbackData);
            } else {
                this.amountInCached.set(amountToPay);
                tokenIn = tokenOut; // swap in/out because exact output swaps are reversed
                PeripheryPayments.pay(this.wICX, tokenIn, callbackData.payer, caller, amountToPay);
            }
        }
    }

    /**
     * @notice Swaps `amountIn` of one token for as much as possible of another token
     * @param params The parameters necessary for the swap, encoded as `ExactInputSingleParams` in calldata
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

        PoolData pool = Path.decodeFirstPool(new ByteReader(data.path));
        Address tokenIn = pool.tokenA;
        Address tokenOut = pool.tokenB;
        int fee = pool.fee;

        boolean zeroForOne = AddressUtils.compareTo(tokenIn, tokenOut) < 0;

        var result = (PairAmounts) Context.call(getPool(tokenIn, tokenOut, fee), "swap", 
            recipient,
            zeroForOne,
            amountIn,
            sqrtPriceLimitX96.equals(ZERO)
                ? (zeroForOne ? TickMath.MIN_SQRT_RATIO.add(ONE) : TickMath.MAX_SQRT_RATIO.subtract(ONE))
                : sqrtPriceLimitX96,
            data.toBytes()
        );

        return zeroForOne ? result.amount1.negate() : result.amount0.negate();
    }

    /**
     * @notice Swaps `amountIn` of one token for as much as possible of another token
     * @param params The parameters necessary for the swap, encoded as `ExactInputSingleParams` in calldata
     * @return amountOut The amount of the received token
     */
    // @External - this method is external through tokenFallback
    public BigInteger exactInputSingle (Address caller, Address tokenIn, BigInteger amountIn, ExactInputSingleParams params) {
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

        return amountOut;
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

            default:
                Context.revert("tokenFallback: Unimplemented tokenFallback action");
        }
    }

    @External
    public BigInteger exactInput (ExactInputParams params) {
        this.checkDeadline(params.deadline);
        BigInteger amountOut = ZERO;
        
        Address payer = Context.getCaller(); // caller pays for the first hop

        while (true) {
            boolean hasMultiplePools = Path.hasMultiplePools(params.path);

            // the outputs of prior swaps become the inputs to subsequent ones
            params.amountIn = exactInputInternal(
                params.amountIn, 
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
                amountOut = params.amountIn;
                break;
            }
        }

        Context.require(amountOut.compareTo(params.amountOutMinimum) >= 0, 
            "exactInput: Too little received");

        return amountOut;
    }

    /**
     * @notice Swaps as little as possible of one token for `amountOut` of another token
     * @param params The parameters necessary for the swap, encoded as `ExactOutputSingleParams` in calldata
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

        PoolData pool = Path.decodeFirstPool(new ByteReader(data.path));
        Address tokenOut = pool.tokenA;
        Address tokenIn = pool.tokenB;
        int fee = pool.fee;

        boolean zeroForOne = AddressUtils.compareTo(tokenIn, tokenOut) < 0;

        // TODO: token transfer

        var result = (PairAmounts) Context.call(getPool(tokenIn, tokenOut, fee), "swap", 
            recipient,
            zeroForOne,
            amountOut.negate(),
            sqrtPriceLimitX96.equals(ZERO)
                ? (zeroForOne ? TickMath.MIN_SQRT_RATIO.add(ONE) : TickMath.MAX_SQRT_RATIO.subtract(ONE))
                : sqrtPriceLimitX96,
            data.toBytes()
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

    /**
     * 
     * @notice Swaps as little as possible of one token for `amountOut` of another token
     * @param params The parameters necessary for the swap, encoded as `ExactOutputSingleParams` in calldata
     * @return amountIn The amount of the input token
     */
    @External
    public BigInteger exactOutputSingle (ExactOutputSingleParams params) {
        this.checkDeadline(params.deadline);
        
        final Address caller = Context.getCaller(); // caller pays for the first hop

        // avoid an db loading by using the swap return data
        BigInteger amountIn = exactOutputInternal(
            params.amountOut, 
            params.recipient, 
            params.sqrtPriceLimitX96, 
            new SwapCallbackData(
                BytesUtils.concat(
                    params.tokenOut.toByteArray(), 
                    BytesUtils.intToBytes(params.fee), 
                    params.tokenIn.toByteArray()
                ), 
                caller
            )
        );

        Context.require(amountIn.compareTo(params.amountInMaximum) <= 0, 
            "exactOutputSingle: Too much requested");

        // has to be reset even though we don't use it in the single hop case
        this.amountInCached.set(DEFAULT_AMOUNT_IN_CACHED);

        return amountIn;
    }

    @External
    public BigInteger exactOutput(ExactOutputParams params) {
        this.checkDeadline(params.deadline);
        
        // it's okay that the payer is fixed to msg.sender here, as they're only paying for the "final" exact output
        // swap, which happens first, and subsequent swaps are paid for within nested callback frames
        exactOutputInternal(
            params.amountOut, 
            params.recipient, 
            ZERO, 
            new SwapCallbackData(params.path, Context.getCaller())
        );

        BigInteger amountIn = amountInCached.get();

        Context.require(amountIn.compareTo(params.amountInMaximum) <= 0, 
            "exactOutput: Too much requested");

        amountInCached.set(DEFAULT_AMOUNT_IN_CACHED);

        return amountIn;
    }

    // ================================================
    // Checks
    // ================================================
    /**
     * Check if transaction hasn't reached the deadline
     * @param deadline
     */
    private void checkDeadline(BigInteger deadline) {
        Context.require(_blockTimestamp().compareTo(deadline) <= 0,
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
