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

package exchange.switchy.quoter;

import static exchange.switchy.utils.IntConstants.MAX_UINT256;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import exchange.switchy.librairies.CallbackValidation;
import exchange.switchy.librairies.Path;
import exchange.switchy.librairies.PoolAddress;
import exchange.switchy.librairies.PoolData;
import exchange.switchy.librairies.TickMath;
import exchange.switchy.utils.AddressUtils;
import exchange.switchy.utils.BytesUtils;
import exchange.switchy.utils.StringUtils;
import exchange.switchy.pool.Slot0;
import score.Address;
import score.Context;
import score.UserRevertedException;
import score.VarDB;
import score.annotation.External;
import scorex.util.StringTokenizer;

class SwitchySwapCallbackException extends Exception {
    SwitchySwapCallbackException (String error) {
        super(error);
    }
}

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
    
    // Contract class name
    private static final String NAME = "Quoter";

    // Contract name
    private final String name;
    private final Address factory;

    /// @dev Transient storage variable used to check a safety condition in exact output swaps.
    private final VarDB<BigInteger> amountOutCached = Context.newVarDB(NAME + "_amountOutCached", BigInteger.class);

    // ================================================
    // DB Variables
    // ================================================

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public Quoter(
        Address _factory
    ) {
        this.name = "Switchy Quoter";
        this.factory = _factory;
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
        byte[] path
    ) throws SwitchySwapCallbackException {
        Context.require(
            amount0Delta.compareTo(ZERO) > 0 
        ||  amount1Delta.compareTo(ZERO) > 0, 
            "switchySwapCallback: swaps entirely within 0-liquidity regions are not supported");

        PoolData decoded = Path.decodeFirstPool(path);
        Address tokenIn = decoded.tokenA;
        Address tokenOut = decoded.tokenB;
        int fee = decoded.fee;

        CallbackValidation.verifyCallback(this.factory, tokenIn, tokenOut, fee);

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

        Address pool = getPool(tokenIn, tokenOut, fee);
        var slot0 = (Slot0) Context.call(pool, "slot0");
        BigInteger sqrtPriceX96After = slot0.sqrtPriceX96;
        int tickAfter = slot0.tick;

        if (isExactInput) {
            Context.revert(amountReceived + ";" + sqrtPriceX96After + ";" + tickAfter);
        } else {
            BigInteger amountOutCached = this.amountOutCached.get();
            if (!amountOutCached.equals(ZERO)) {
                Context.require(amountReceived.equals(amountOutCached),
                    "switchySwapCallback: amountReceived == amountOutCached");
            }
            Context.revert(amountToPay + ";" + sqrtPriceX96After + ";" + tickAfter);
        }
    }

    class RevertReason {
        public BigInteger amount;
        public BigInteger sqrtPriceX96After;
        public int tickAfter;
        public RevertReason (
            BigInteger amount,
            BigInteger sqrtPriceX96After,
            int tickAfter
        ) {
            this.amount = amount;
            this.sqrtPriceX96After = sqrtPriceX96After;
            this.tickAfter = tickAfter;
        }
    }

    /**
     * @dev Parses a revert reason that should contain the numeric quote
     */
    private RevertReason parseRevertReason (String reason) {
        StringTokenizer scanner = new StringTokenizer(reason, ";");
        BigInteger amountReceived = StringUtils.toBigInt(scanner.nextToken());
        BigInteger sqrtPriceX96After = StringUtils.toBigInt(scanner.nextToken());
        int tickAfter = StringUtils.toBigInt(scanner.nextToken()).intValue();
        return new RevertReason(amountReceived, sqrtPriceX96After, tickAfter);
    }

    class QuoteResult {
        public BigInteger amount;
        public BigInteger sqrtPriceX96After;
        public int initializedTicksCrossed;

        public QuoteResult(
            BigInteger amount,
            BigInteger sqrtPriceX96After,
            int initializedTicksCrossed
        ) {
            this.amount = amount;
            this.sqrtPriceX96After = sqrtPriceX96After;
            this.initializedTicksCrossed = initializedTicksCrossed;
        }
    }

    class QuoteMultiResult {
        public BigInteger amount;
        public BigInteger[] sqrtPriceX96AfterList;
        public int[] initializedTicksCrossedList;

        public QuoteMultiResult(
            BigInteger amount,
            BigInteger[] sqrtPriceX96AfterList,
            int[] initializedTicksCrossedList
        ) {
            this.amount = amount;
            this.sqrtPriceX96AfterList = sqrtPriceX96AfterList;
            this.initializedTicksCrossedList = initializedTicksCrossedList;
        }
    }

    private QuoteResult handleRevert (
        String reason,
        Address pool
    ) {
        var slot0 = (Slot0) Context.call(pool, "slot0");
        int tickBefore = slot0.tick;
        var result = parseRevertReason(reason);

        int initializedTicksCrossed = countInitializedTicksCrossed(pool, tickBefore, result.tickAfter);
        
        return new QuoteResult(result.amount, result.sqrtPriceX96After, initializedTicksCrossed);
    }

    private BigInteger tickBitmap (Address pool, int pos) {
        return (BigInteger) Context.call(pool, "tickBitmap", pos);
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
        int tickSpacing = ((BigInteger) Context.call(pool, "tickSpacing")).intValue();
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

    class QuoteExactInputSingleParams {
        // The token being swapped in
        public Address tokenIn;
        // The token being swapped out
        public Address tokenOut;
        // The desired input amount
        public BigInteger amountIn;
        // The fee of the token pool to consider for the pair
        public int fee;
        // The price limit of the pool that cannot be exceeded by the swap
        public BigInteger sqrtPriceLimitX96;

        public QuoteExactInputSingleParams (
            Address tokenIn,
            Address tokenOut,
            BigInteger amountIn,
            int fee,
            BigInteger sqrtPriceLimitX96
        ) {
            this.tokenIn = tokenIn;
            this.tokenOut = tokenOut;
            this.amountIn = amountIn;
            this.fee = fee;
            this.sqrtPriceLimitX96 = sqrtPriceLimitX96;
        }
    }

    /**
     * @notice Returns the amount out received for a given exact input but for a swap of a single pool
     * @param params The params for the quote, encoded as `QuoteExactInputSingleParams`
     * @return amountOut The amount of `tokenOut` that would be received
     * @return sqrtPriceX96After The sqrt price of the pool after the swap
     * @return initializedTicksCrossed The number of initialized ticks that the swap crossed
     * @return gasEstimate The estimate of the gas that the swap consumes
     */
    @External
    public QuoteResult quoteExactInputSingle (QuoteExactInputSingleParams params) {
        boolean zeroForOne = AddressUtils.compareTo(params.tokenIn, params.tokenOut) < 0;
        Address pool = getPool(params.tokenIn, params.tokenOut, params.fee);

        try {
            Context.call(pool, "swap",
                Context.getAddress(), // ZERO_ADDRESS might cause issues with some tokens
                zeroForOne,
                params.amountIn,
                params.sqrtPriceLimitX96.equals(ZERO)
                    ? (zeroForOne ? TickMath.MIN_SQRT_RATIO.add(ONE) : TickMath.MAX_SQRT_RATIO.subtract(ONE))
                    : params.sqrtPriceLimitX96,
                BytesUtils.concat(
                    params.tokenIn.toByteArray(), 
                    BytesUtils.intToBytes(params.fee), 
                    params.tokenOut.toByteArray())
            );
        } catch (UserRevertedException exception) {
            return handleRevert(exception.getMessage(), pool);
        }

        return null;
    }

    /// @notice Returns the amount out received for a given exact input swap without executing the swap
    /// @param path The path of the swap, i.e. each token pair and the pool fee
    /// @param amountIn The amount of the first token to swap
    /// @return amountOut The amount of the last token that would be received
    /// @return sqrtPriceX96AfterList List of the sqrt price after the swap for each pool in the path
    /// @return initializedTicksCrossedList List of the initialized ticks that the swap crossed for each pool in the path
    /// @return gasEstimate The estimate of the gas that the swap consumes
    @External
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
            
            BigInteger _amountOut = result.amount;
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

    class QuoteExactOutputSingleParams {
        // The token being swapped in
        public Address tokenIn;
        // The token being swapped out
        public Address tokenOut;
        // The desired output amount
        public BigInteger amount;
        // The fee of the token pool to consider for the pair
        public int fee;
        // The price limit of the pool that cannot be exceeded by the swap
        public BigInteger sqrtPriceLimitX96;

        public QuoteExactOutputSingleParams (
            Address tokenIn,
            Address tokenOut,
            BigInteger amount,
            int fee,
            BigInteger sqrtPriceLimitX96
        ) {
            this.tokenIn = tokenIn;
            this.tokenOut = tokenOut;
            this.amount = amount;
            this.fee = fee;
            this.sqrtPriceLimitX96 = sqrtPriceLimitX96;
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
    @External
    public QuoteResult quoteExactOutputSingle (QuoteExactOutputSingleParams params) {
        boolean zeroForOne = AddressUtils.compareTo(params.tokenIn, params.tokenOut) < 0;
        Address pool = getPool(params.tokenIn, params.tokenOut, params.fee);
        
        // if no price limit has been specified, cache the output amount for comparison in the swap callback
        if (params.sqrtPriceLimitX96.equals(ZERO)) {
            amountOutCached.set(params.amount);
        }

        try {
            Context.call(pool, "swap",
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
        } catch (UserRevertedException exception) {
            if (params.sqrtPriceLimitX96.equals(ZERO)) {
                amountOutCached.set(null); // clear cache
            }
            return handleRevert(exception.getMessage(), pool);
        }

        return null;
    }

    /**
     * @notice Returns the amount in required for a given exact output swap without executing the swap
     * @param path The path of the swap, i.e. each token pair and the pool fee. Path must be provided in reverse order
     * @param amountOut The amount of the last token to receive
     * @return amountIn The amount of first token required to be paid
     */
    @External
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
            
            BigInteger _amountIn = result.amount;
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
