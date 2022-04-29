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

package exchange.convexus.periphery.quoter;

import static exchange.convexus.utils.IntUtils.MAX_UINT256;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import exchange.convexus.librairies.TickMath;
import exchange.convexus.periphery.librairies.Path;
import exchange.convexus.periphery.librairies.PoolAddressLib;
import exchange.convexus.periphery.poolreadonly.SwapResult;
import exchange.convexus.utils.AddressUtils;
import exchange.convexus.utils.BytesUtils;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.PoolData;
import exchange.convexus.poolreadonly.IConvexusPoolReadOnly;
import score.Address;
import score.Context;
import score.annotation.External;

/**
 * @title Provides quotes for swaps
 * @notice Allows getting the expected amount out or amount in for a given swap without executing the swap
 * @dev These functions are not gas efficient and should _not_ be called on chain. Instead, optimistically execute
 * the swap and check the amounts in the callback.
 */
public class Quoter {

    // ================================================
    // Consts
    // ================================================
    // Contract name
    private final String name;
    private final Address factory;
    private final Address readOnlyPool;

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public Quoter (
        Address factory,
        Address readOnlyPool
    ) {
        this.name = "Convexus Quoter";
        this.factory = factory;
        this.readOnlyPool = readOnlyPool;
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

    @External(readonly = true)
    public SwapResult convexusSwapCallbackReadonly (
        BigInteger amount0Delta,
        BigInteger amount1Delta,
        byte[] path,
        BigInteger sqrtPriceX96,
        int tick
    ) {
        Context.require(
            amount0Delta.compareTo(ZERO) > 0 
        ||  amount1Delta.compareTo(ZERO) > 0, 
            "convexusSwapCallback: swaps entirely within 0-liquidity regions are not supported");

        PoolData decoded = Path.decodeFirstPool(path);
        Address tokenIn = decoded.tokenA;
        Address tokenOut = decoded.tokenB;

        boolean isExactInput;
        BigInteger amountToPay;
        BigInteger amountReceived;
        if (amount0Delta.compareTo(ZERO) > 0) {
            isExactInput = AddressUtils.compareTo(tokenIn, tokenOut) < 0;
            amountToPay = amount0Delta;
            amountReceived = amount1Delta.negate();
        } else {
            isExactInput = AddressUtils.compareTo(tokenOut, tokenIn) < 0;
            amountToPay = amount1Delta;
            amountReceived = amount0Delta.negate();
        }

        BigInteger sqrtPriceX96After = sqrtPriceX96;
        int tickAfter = tick;
        BigInteger amountOut = isExactInput ? amountReceived : amountToPay;
        Context.println("amountOut=" + amountOut);
        Context.println("sqrtPriceX96After=" + sqrtPriceX96After);
        Context.println("tickAfter=" + tickAfter);
        return new SwapResult(amountOut, sqrtPriceX96After, tickAfter);
    }

    private QuoteResult handleResult (
        SwapResult swapResult,
        Address pool
    ) {
        var slot0 = IConvexusPool.slot0(pool);
        int tickBefore = slot0.tick;
        int initializedTicksCrossed = countInitializedTicksCrossed(pool, tickBefore, swapResult.tickAfter);
        
        return new QuoteResult(swapResult.amount, swapResult.sqrtPriceX96After, initializedTicksCrossed);
    }

    private BigInteger tickBitmap (Address pool, int pos) {
        return IConvexusPool.tickBitmap(pool, pos);
    }

    /**
     * @dev This function counts the number of initialized ticks that would incur a gas cost between tickBefore and tickAfter.
     * When tickBefore and/or tickAfter themselves are initialized, the logic over whether we should count them depends on the
     * direction of the swap. If we are swapping upwards (tickAfter > tickBefore) we don't want to count tickBefore but we do
     * want to count tickAfter. The opposite is true if we are swapping downwards.
     */
    private int countInitializedTicksCrossed(Address pool, int tickBefore, int tickAfter) {
        int initializedTicksCrossed = 0;

        // Get the key and offset in the tick bitmap of the active tick before and after the swap.
        int tickSpacing = IConvexusPool.tickSpacing(pool);
        int wordPos = (tickBefore / tickSpacing) >> 8;
        int bitPos = (tickBefore / tickSpacing) % 256;

        int wordPosAfter = (tickAfter / tickSpacing) >> 8;
        int bitPosAfter = (tickAfter / tickSpacing) % 256;

        int wordPosLower;
        int wordPosHigher;
        int bitPosLower;
        int bitPosHigher;

        // In the case where tickAfter is initialized, we only want to count it if we are swapping downwards.
        // If the initializable tick after the swap is initialized, our original tickAfter is a
        // multiple of tick spacing, and we are swapping downwards we know that tickAfter is initialized
        // and we shouldn't count it.
        boolean tickAfterInitialized =
            ((this.tickBitmap(pool, wordPosAfter).and(ONE.shiftLeft(bitPosAfter))).compareTo(ZERO) > 0) &&
            ((tickAfter % tickSpacing) == 0) &&
            (tickBefore > tickAfter);

        // In the case where tickBefore is initialized, we only want to count it if we are swapping upwards.
        // Use the same logic as above to decide whether we should count tickBefore or not.
        boolean tickBeforeInitialized =
            ((this.tickBitmap(pool, wordPos).and(ONE.shiftLeft(bitPos))).compareTo(ZERO) > 0) &&
            ((tickBefore % tickSpacing) == 0) &&
            (tickBefore < tickAfter);

        if (wordPos < wordPosAfter || (wordPos == wordPosAfter && bitPos <= bitPosAfter)) {
            wordPosLower = wordPos;
            bitPosLower = bitPos;
            wordPosHigher = wordPosAfter;
            bitPosHigher = bitPosAfter;
        } else {
            wordPosLower = wordPosAfter;
            bitPosLower = bitPosAfter;
            wordPosHigher = wordPos;
            bitPosHigher = bitPos;
        }

        // Count the number of initialized ticks crossed by iterating through the tick bitmap.
        // Our first mask should include the lower tick and everything to its left.
        // @dev: No int overflowing in Java-SCORE.
        BigInteger mask = MAX_UINT256.subtract(ONE.shiftLeft(bitPosLower).subtract(ONE));

        while (wordPosLower <= wordPosHigher) {
            // If we're on the final tick bitmap page, ensure we only count up to our
            // ending tick.
            if (wordPosLower == wordPosHigher) {
                mask = mask.and(MAX_UINT256.shiftRight(255 - bitPosHigher));
            }

            BigInteger masked = tickBitmap(pool, wordPosLower).and(mask);
            initializedTicksCrossed += countOneBits(masked);
            wordPosLower++;
            // Reset our mask so we consider all bits on the next iteration.
            mask = MAX_UINT256;
        }

        if (tickAfterInitialized) {
            initializedTicksCrossed -= 1;
        }

        if (tickBeforeInitialized) {
            initializedTicksCrossed -= 1;
        }

        return initializedTicksCrossed;
    }

    private int countOneBits(BigInteger x) {
        int bits = 0;
        while (!x.equals(ZERO)) {
            bits++;
            x = x.and(x.subtract(ONE));
        }
        return bits;
    }

    /**
     * @notice Returns the amount out received for a given exact input but for a swap of a single pool
     * @param params The params for the quote, encoded as `QuoteExactInputSingleParams`
     * @return amountOut The amount of `tokenOut` that would be received
     * @return sqrtPriceX96After The sqrt price of the pool after the swap
     * @return initializedTicksCrossed The number of initialized ticks that the swap crossed
     * @return gasEstimate The estimate of the gas that the swap consumes
     */
    @External(readonly = true)
    public QuoteResult quoteExactInputSingle (QuoteExactInputSingleParams params) {
        boolean zeroForOne = AddressUtils.compareTo(params.tokenIn, params.tokenOut) < 0;
        Address pool = getPool(params.tokenIn, params.tokenOut, params.fee);

        var result = IConvexusPoolReadOnly.swap(this.readOnlyPool,
                pool, 
                Context.getAddress(), // ZERO_ADDRESS might cause issues with some tokens
                zeroForOne, 
                params.amountIn,
                params.sqrtPriceLimitX96.equals(ZERO)
                    ? (zeroForOne ? TickMath.MIN_SQRT_RATIO.add(ONE) : TickMath.MAX_SQRT_RATIO.subtract(ONE))
                    : params.sqrtPriceLimitX96,
                Path.encodePath(new PoolData(params.tokenIn, params.tokenOut, params.fee))
            );
        
        return handleResult(result, pool);
    }

    /// @notice Returns the amount out received for a given exact input swap without executing the swap
    /// @param path The path of the swap, i.e. each token pair and the pool fee
    /// @param amountIn The amount of the first token to swap
    /// @return amountOut The amount of the last token that would be received
    /// @return sqrtPriceX96AfterList List of the sqrt price after the swap for each pool in the path
    /// @return initializedTicksCrossedList List of the initialized ticks that the swap crossed for each pool in the path
    /// @return gasEstimate The estimate of the gas that the swap consumes
    @External(readonly = true)
    public QuoteMultiResult quoteExactInput (byte[] path, BigInteger amountIn) {

        int numPools = Path.numPools(path);
        BigInteger[] sqrtPriceX96AfterList = new BigInteger[numPools];
        int[] initializedTicksCrossedList = new int[numPools];

        int i = 0;
        while (true) {
            var firstPool = Path.decodeFirstPool(path);
            Address tokenIn = firstPool.tokenA;
            Address tokenOut = firstPool.tokenB;
            int fee = firstPool.fee;

            // the outputs of prior swaps become the inputs to subsequent ones
            var result =
                quoteExactInputSingle(
                    new QuoteExactInputSingleParams(
                        tokenIn,
                        tokenOut,
                        amountIn,
                        fee,
                        ZERO
                    )
                );
            
            BigInteger _amountOut = result.amountOut;
            BigInteger _sqrtPriceX96After = result.sqrtPriceX96After;
            int _initializedTicksCrossed = result.initializedTicksCrossed;
            
            sqrtPriceX96AfterList[i] = _sqrtPriceX96After;
            initializedTicksCrossedList[i] = _initializedTicksCrossed;
            amountIn = _amountOut;
            i++;
            
            // decide whether to continue or terminate
            if (Path.hasMultiplePools(path)) {
                path = Path.skipToken(path);
            } else {
                return new QuoteMultiResult(amountIn, sqrtPriceX96AfterList, initializedTicksCrossedList);
            }
        }
    }

    /**
     * @notice Returns the amount in required to receive the given exact output amount but for a swap of a single pool
     * @param params The params for the quote, encoded as `QuoteExactOutputSingleParams`
     * @return amountIn The amount required as the input for the swap in order to receive `amountOut`
     * @return sqrtPriceX96After The sqrt price of the pool after the swap
     * @return initializedTicksCrossed The number of initialized ticks that the swap crossed
     * @return gasEstimate The estimate of the gas that the swap consumes
     */
    @External(readonly = true)
    public QuoteResult quoteExactOutputSingle (QuoteExactOutputSingleParams params) {
        boolean zeroForOne = AddressUtils.compareTo(params.tokenIn, params.tokenOut) < 0;
        Address pool = getPool(params.tokenIn, params.tokenOut, params.fee);

        var result = IConvexusPoolReadOnly.swap(this.readOnlyPool,
            pool, 
            Context.getAddress(), // ZERO_ADDRESS might cause issues with some tokens
            zeroForOne,
            params.amount.negate(),
            params.sqrtPriceLimitX96.equals(ZERO)
                ? (zeroForOne ? TickMath.MIN_SQRT_RATIO.add(ONE) : TickMath.MAX_SQRT_RATIO.subtract(ONE))
                : params.sqrtPriceLimitX96,
            BytesUtils.concat(
                params.tokenOut.toByteArray(), 
                BytesUtils.intToBytes(params.fee), 
                params.tokenIn.toByteArray())
        );

        return handleResult(result, pool);
    }

    /**
     * @notice Returns the amount in required for a given exact output swap without executing the swap
     * @param path The path of the swap, i.e. each token pair and the pool fee. Path must be provided in reverse order
     * @param amountOut The amount of the last token to receive
     * @return amountIn The amount of first token required to be paid
     */
    @External(readonly = true)
    public QuoteMultiResult quoteExactOutput (byte[] path, BigInteger amountOut) {
        
        int numPools = Path.numPools(path);
        BigInteger[] sqrtPriceX96AfterList = new BigInteger[numPools];
        int[] initializedTicksCrossedList = new int[numPools];

        int i = 0;
        while (true) {
            var firstPool = Path.decodeFirstPool(path);
            Address tokenIn = firstPool.tokenA;
            Address tokenOut = firstPool.tokenB;
            int fee = firstPool.fee;
            
            // the inputs of prior swaps become the outputs of subsequent ones
            var result =
            quoteExactOutputSingle(
                    new QuoteExactOutputSingleParams(
                        tokenIn,
                        tokenOut,
                        amountOut,
                        fee,
                        ZERO
                    )
                );
            
            BigInteger _amountIn = result.amountOut;
            BigInteger _sqrtPriceX96After = result.sqrtPriceX96After;
            int _initializedTicksCrossed = result.initializedTicksCrossed;
            
            sqrtPriceX96AfterList[i] = _sqrtPriceX96After;
            initializedTicksCrossedList[i] = _initializedTicksCrossed;
            amountOut = _amountIn;
            i++;
            
            // decide whether to continue or terminate
            if (Path.hasMultiplePools(path)) {
                path = Path.skipToken(path);
            } else {
                return new QuoteMultiResult(amountOut, sqrtPriceX96AfterList, initializedTicksCrossedList);
            }
        }
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
