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

import exchange.switchy.librairies.CallbackValidation;
import exchange.switchy.librairies.LiquidityAmounts;
import exchange.switchy.librairies.PairAmounts;
import exchange.switchy.librairies.PeripheryPayments;
import exchange.switchy.librairies.PoolAddress;
import exchange.switchy.librairies.TickMath;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.annotation.External;

import exchange.switchy.pool.Slot0;
import exchange.switchy.utils.ByteReader;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import score.annotation.Optional;
import scorex.io.Reader;
import scorex.io.StringReader;

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
    // address of sICX
    public final Address sICX;

    // ================================================
    // DB Variables
    // ================================================
    // User => Token => Amount
    private final BranchDB<Address, DictDB<Address, BigInteger>> deposited = Context.newBranchDB(NAME + "_deposited", BigInteger.class);

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
        Address _sICX
    ) {
        this.name = "Switchy Liquidity Management";
        this.factory = _factory;
        this.sICX = _sICX;
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
        MintCallbackData decoded = MintCallbackData.fromBytes(new ByteReader(data));
        CallbackValidation.verifyCallback(this.factory, decoded.poolKey);

        if (amount0Owed.compareTo(ZERO) > 0) {
            pay(decoded.payer, decoded.poolKey.token0, amount0Owed);
        }

        if (amount1Owed.compareTo(ZERO) > 0) {
            pay(decoded.payer, decoded.poolKey.token1, amount1Owed);
        }
    }

    private void pay (Address payer, Address token, BigInteger owed) {
        final Address caller = Context.getCaller();
        checkEnoughDeposited(payer, token, owed);
        PeripheryPayments.pay(this.sICX, token, caller, owed);
    }

    /**
     * @notice Add liquidity to an initialized pool
     */
    @External
    public AddLiquidityResult addLiquidity (AddLiquidityParams params) {
        PoolAddress.PoolKey poolKey = new PoolAddress.PoolKey(params.token0, params.token1, params.fee);

        Address pool = PoolAddress.getPool(this.factory, poolKey);

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

    // @External - this method is external through tokenFallback
    private void deposit (Address caller, Address tokenIn, BigInteger amountIn) {
        // --- Checks ---
        Context.require(amountIn.compareTo(ZERO) > 0, 
            "deposit: Deposit amount cannot be less or equal to 0");

        // --- OK from here ---
        var deposited = this.deposited.at(caller);
        deposited.set(caller, deposited.get(caller).add(amountIn));
    }

    @External
    public void withdraw (Address token) {
        final Address caller = Context.getCaller();

        var deposited = this.deposited.at(caller);
        BigInteger amount = deposited.get(caller);

        if (amount.compareTo(ZERO) > 0) {
            Context.call(token, "transfer", caller, amount, "withdraw".getBytes());
            deposited.set(caller, ZERO);
        }
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
            case "deposit": {
                deposit(_from, token, _value);
                break;
            }

            default:
                Context.revert("tokenFallback: Unimplemented tokenFallback action");
        }
    }

    // ================================================
    // Checks
    // ================================================
    private void checkEnoughDeposited (Address address, Address token, BigInteger amount) {
        var deposited = this.deposited.at(address);
        Context.require(deposited.get(token).compareTo(amount) >= 0,
            "checkEnoughDeposited: user didn't deposit enough funds");
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
    
    @External(readonly = true)
    public BigInteger deposited(Address user, Address token) {
        return this.deposited.at(user).get(token);
    }
}
