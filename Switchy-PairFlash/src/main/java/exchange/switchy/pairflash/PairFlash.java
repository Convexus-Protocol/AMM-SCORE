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

package exchange.switchy.pairflash;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import exchange.switchy.librairies.CallbackValidation;
import exchange.switchy.librairies.PeripheryPayments;
import exchange.switchy.librairies.PoolAddress;
import exchange.switchy.router.ExactInputSingleParams;
import exchange.switchy.utils.ByteReader;
import exchange.switchy.utils.TimeUtils;
import score.Address;
import score.Context;
import score.annotation.External;

/**
 * @title Flash contract implementation
 * @notice An example contract using the Switchy flash function
 */
public class PairFlash {

    // ================================================
    // Consts
    // ================================================
    // Contract name
    private final String name;
    private final Address factory;
    private final Address sICX;
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
        Address _factory,
        Address _sICX
    ) {
        this.name = "Switchy Pair Flash";
        this.swapRouter = _swapRouter;
        this.sICX = _sICX;
        this.factory = _factory;
    }

    /**
     * @param fee0 The fee from calling flash for token0
     * @param fee1 The fee from calling flash for token1
     * @param data The data needed in the callback passed as FlashCallbackData from `initFlash`
     * @notice implements the callback called from flash
     * @dev fails if the flash is not profitable, meaning the amountOut from the flash is less than the amount borrowed
     */
    @External
    public void switchyFlashCallback (
        BigInteger fee0,
        BigInteger fee1,
        byte[] data
    ) {
        FlashCallbackData decoded = FlashCallbackData.fromBytes(new ByteReader(data));
        CallbackValidation.verifyCallback(this.factory, decoded.poolKey);
        
        final Address thisAddress = Context.getAddress();
        final Address caller = Context.getCaller();

        Address token0 = decoded.poolKey.token0;
        Address token1 = decoded.poolKey.token1;

        // profitability parameters - we must receive at least the required payment from the arbitrage swaps
        // exactInputSingle will fail if this amount not met
        BigInteger amount0Min = decoded.amount0.add(fee0);
        BigInteger amount1Min = decoded.amount1.add(fee1);

        // call exactInputSingle for swapping token1 for token0 in pool with fee2
        BigInteger amountOut0 = (BigInteger)
            Context.call(swapRouter, "exactInputSingle", new ExactInputSingleParams(
                token0, 
                decoded.poolFee2, 
                thisAddress, 
                TimeUtils.nowSeconds(), 
                amount0Min, 
                ZERO
            ));
            
        // call exactInputSingle for swapping token0 for token 1 in pool with fee3
        BigInteger amountOut1 = (BigInteger)
            Context.call(swapRouter, "exactInputSingle", new ExactInputSingleParams(
                token1,
                decoded.poolFee3,
                thisAddress, 
                TimeUtils.nowSeconds(), 
                amount1Min,
                ZERO
            )
        );
        
        // pay the required amounts back to the pair
        if (amount0Min.compareTo(ZERO) > 0) {
            PeripheryPayments.pay(this.sICX, token0, thisAddress, caller, amount0Min);
        }
        if (amount1Min.compareTo(ZERO) > 0) {
            PeripheryPayments.pay(this.sICX, token1, thisAddress, caller, amount1Min);
        }

        // if profitable pay profits to payer
        if (amountOut0.compareTo(amount0Min) > 0) {
            BigInteger profit0 = amountOut0.subtract(amount0Min);
            PeripheryPayments.pay(this.sICX, token0, thisAddress, decoded.payer, profit0);
        }
        if (amountOut1.compareTo(amount1Min) > 0) {
            BigInteger profit1 = amountOut1.subtract(amount1Min);
            PeripheryPayments.pay(this.sICX, token1, thisAddress, decoded.payer, profit1);
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
     * @notice Calls the pools flash function with data needed in `uniswapV3FlashCallback`
     */
    @External
    public void initFlash (FlashParams params) {
        PoolAddress.PoolKey poolKey = new PoolAddress.PoolKey(params.token0, params.token1, params.fee1);
        Address pool = PoolAddress.getPool(this.factory, poolKey);

        final Address thisAddress = Context.getAddress();
        final Address caller = Context.getCaller();

        // recipient of borrowed amounts
        // amount of token0 requested to borrow
        // amount of token1 requested to borrow
        // need amount 0 and amount1 in callback to pay back pool
        // recipient of flash should be THIS contract
        Context.call(pool, "flash", 
            thisAddress,
            params.amount0,
            params.amount1,
            new FlashCallbackData(
                params.amount0,
                params.amount1,
                caller,
                poolKey,
                params.fee2,
                params.fee3
            ).toBytes()
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
