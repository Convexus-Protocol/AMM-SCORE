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

package exchange.switchy.liquidity;

import java.math.BigInteger;
import java.util.Arrays;

import exchange.switchy.librairies.CallbackValidation;
import exchange.switchy.librairies.LiquidityAmounts;
import exchange.switchy.librairies.PairAmounts;
import exchange.switchy.librairies.PeripheryPayments;
import exchange.switchy.librairies.PoolAddress;
import exchange.switchy.librairies.TickMath;
import score.Address;
import score.Context;
import score.annotation.External;

import exchange.switchy.pool.Slot0;
import exchange.switchy.utils.BytesUtils;

class MintCallbackData {
    PoolAddress.PoolKey poolKey;
    Address payer;

    public MintCallbackData (PoolAddress.PoolKey poolKey, Address payer) {
        this.poolKey = poolKey;
        this.payer = payer;
    }

    public static MintCallbackData fromBytes(byte[] data) {
        int offset = 0;
        PoolAddress.PoolKey poolKey = PoolAddress.PoolKey.fromBytes(Arrays.copyOfRange(data, offset, offset + PoolAddress.PoolKey.LENGTH));
        offset += PoolAddress.PoolKey.LENGTH;
        Address payer = new Address(Arrays.copyOfRange(data, offset, offset + Address.LENGTH));
        return new MintCallbackData(poolKey, payer);
    }

    public byte[] toBytes () {
        return BytesUtils.concat(
            this.poolKey.toBytes(),
            this.payer.toByteArray()
        );
    }
}

public class SwitchyLiquidityManagement {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    private static final String NAME = "SwitchyLiquidityManagement";

    // Contract name
    private final String name;

    // address of the Switchy factory
    public final Address factory;
    // address of wICX
    public final Address wICX;

    // ================================================
    // DB Variables
    // ================================================

    // ================================================
    // Event Logs
    // ================================================

    // ================================================
    // Methods
    // ================================================
    /**
     *  @notice Contract constructor
     */
    public SwitchyLiquidityManagement(
        Address _factory,
        Address _wICX
    ) {
        this.name = "Switchy Liquidity Management";
        this.factory = _factory;
        this.wICX = _wICX;
    }

    /**
     * @notice Called to `msg.sender` after minting liquidity to a position from SwitchyPool#mint.
     * @dev In the implementation you must pay the pool tokens owed for the minted liquidity.
     * The caller of this method must be checked to be a SwitchyPool deployed by the canonical SwitchyFactory.
     * @param amount0Owed The amount of token0 due to the pool for the minted liquidity
     * @param amount1Owed The amount of token1 due to the pool for the minted liquidity
     * @param data Any data passed through by the caller via the mint call
     */
    @External
    public void switchyMintCallback (
        BigInteger amount0Owed,
        BigInteger amount1Owed,
        byte[] data
    ) {
        MintCallbackData decoded = MintCallbackData.fromBytes(data);
        CallbackValidation.verifyCallback(factory, decoded.poolKey);

        final Address caller = Context.getCaller();

        if (amount0Owed.compareTo(BigInteger.ZERO) > 0) {
            PeripheryPayments.pay(this.wICX, decoded.poolKey.token0, decoded.payer, caller, amount0Owed);
        }

        if (amount1Owed.compareTo(BigInteger.ZERO) > 0) {
            PeripheryPayments.pay(this.wICX, decoded.poolKey.token1, decoded.payer, caller, amount1Owed);
        }
    }

    /**
     * @notice Add liquidity to an initialized pool
     */
    @External
    public AddLiquidityResult addLiquidity (AddLiquidityParams params) {
        PoolAddress.PoolKey poolKey = new PoolAddress.PoolKey(params.token0, params.token1, params.fee);

        Address pool = PoolAddress.computeAddress(factory, poolKey);

        // compute the liquidity amount
        var result = (Slot0) Context.call(pool, "slot0");
        BigInteger sqrtPriceX96 = result.sqrtPriceX96;
        BigInteger sqrtRatioAX96 = TickMath.getSqrtRatioAtTick(params.tickLower);
        BigInteger sqrtRatioBX96 = TickMath.getSqrtRatioAtTick(params.tickUpper);

        BigInteger liquidity = LiquidityAmounts.getLiquidityForAmounts(
            sqrtPriceX96,
            sqrtRatioAX96,
            sqrtRatioBX96,
            params.amount0Desired,
            params.amount1Desired
        );

        PairAmounts amounts = (PairAmounts) Context.call(pool, "mint", 
            params.recipient,
            params.tickLower,
            params.tickUpper,
            liquidity,
            new MintCallbackData(poolKey, Context.getCaller()).toBytes()
        );

        Context.require(
            amounts.amount0.compareTo(params.amount0Min) >= 0
        &&  amounts.amount1.compareTo(params.amount1Min) >= 0,
            "addLiquidity: Price slippage check"
        );

        return new AddLiquidityResult (liquidity, amounts.amount0, amounts.amount1, pool);
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly=true)
    public String name() {
        return this.name;
    }
}
