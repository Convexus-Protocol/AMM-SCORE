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

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import exchange.convexus.librairies.CallbackValidation;
import exchange.convexus.librairies.PeripheryPayments;
import exchange.convexus.librairies.PoolAddress;
import exchange.convexus.router.ExactInputSingleParams;
import exchange.convexus.utils.TimeUtils;
import score.Address;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.annotation.External;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * @title Flash contract implementation
 * @notice An example contract using the Convexus flash function
 */
public class PairFlash {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    // private static final String NAME = "ConvexusPairFlash";

    // Contract name
    private final String name;
    private final Address factory;
    private final Address swapRouter;

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
        Address _swapRouter,
        Address _factory
    ) {
        this.name = "Convexus Pair Flash";
        this.swapRouter = _swapRouter;
        this.factory = _factory;
    }

    private BigInteger routerExactInputSingle (Address tokenIn, BigInteger amountIn, Address tokenOut, BigInteger amountOutMinimum, int poolFee) {
        final Address thisAddress = Context.getAddress();

        BigInteger amountBefore = (BigInteger) Context.call(tokenOut, "balanceOf", thisAddress);

        var params = new ExactInputSingleParams(
            tokenOut, 
            poolFee, 
            thisAddress,
            TimeUtils.nowSeconds(), 
            amountOutMinimum,
            ZERO
        );

        // Forward tokenIn to the router and call the "exactInputSingle" method
        JsonObject data = Json.object()
            .add("method", "exactInputSingle")
            .add("params", params.toJson());

        // The call to `exactInputSingle` executes the swap.
        Context.call(tokenIn, "transfer", this.swapRouter, amountIn, data.toString().getBytes());

        BigInteger amountAfter  = (BigInteger) Context.call(tokenOut, "balanceOf", thisAddress);
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

    //fee1 is the fee of the pool from the initial borrow
    //fee2 is the fee of the first pool to arb from
    //fee3 is the fee of the second pool to arb from
    class FlashParams {
        Address token0;
        Address token1;
        int fee1;
        BigInteger amount0;
        BigInteger amount1;
        int fee2;
        int fee3;
    }

    /**
     * @param params The parameters necessary for flash and the callback, passed in as FlashParams
     * @notice Calls the pools flash function with data needed in `convexusFlashCallback`
     */
    @External
    public void initFlash (FlashParams params) {
        PoolAddress.PoolKey poolKey = new PoolAddress.PoolKey(params.token0, params.token1, params.fee1);
        Address pool = PoolAddress.getPool(this.factory, poolKey);

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
        Context.call(pool, "flash", 
            thisAddress,
            params.amount0,
            params.amount1,
            writer.toByteArray()
        );
    }
 
    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
