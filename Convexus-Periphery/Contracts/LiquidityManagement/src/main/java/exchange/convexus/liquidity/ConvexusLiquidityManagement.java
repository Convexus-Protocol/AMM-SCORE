/*
 * Copyright 2022 ICONation
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

package exchange.convexus.liquidity;

import exchange.convexus.interfaces.irc2.IIRC2;
import exchange.convexus.librairies.CallbackValidation;
import exchange.convexus.librairies.LiquidityAmounts;
import exchange.convexus.librairies.MintCallbackData;
import exchange.convexus.librairies.PairAmounts;
import exchange.convexus.librairies.PeripheryPayments;
import exchange.convexus.librairies.PoolAddress;
import exchange.convexus.librairies.TickMath;
import score.Address;
import score.BranchDB;
import score.ByteArrayObjectWriter;
import score.Context;
import score.DictDB;
import score.ObjectReader;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.utils.JSONUtils;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

public class ConvexusLiquidityManagement {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    private static final String NAME = "ConvexusLiquidityManagement";

    // address of the Convexus factory
    public final Address factory;

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
    public ConvexusLiquidityManagement(
        Address _factory
    ) {
        this.factory = _factory;
    }

    /**
     * @notice Called to `Context.getCaller()` after minting liquidity to a position from ConvexusPool#mint.
     * @dev In the implementation you must pay the pool tokens owed for the minted liquidity.
     * The caller of this method must be checked to be a ConvexusPool deployed by the canonical ConvexusFactory.
     * @param amount0Owed The amount of token0 due to the pool for the minted liquidity
     * @param amount1Owed The amount of token1 due to the pool for the minted liquidity
     * @param data Any data passed through by the caller via the mint call
     */
    // @External
    public void convexusMintCallback (
        BigInteger amount0Owed,
        BigInteger amount1Owed,
        byte[] data
    ) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", data);
        MintCallbackData decoded = reader.read(MintCallbackData.class);
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
        
        // Remove funds from deposited
        var depositedUser = this.deposited.at(payer);
        BigInteger oldBalance = depositedUser.getOrDefault(token, ZERO);
        depositedUser.set(token, oldBalance.subtract(owed));
        
        // Actually transfer the tokens
        PeripheryPayments.pay(token, caller, owed);
    }

    /**
     * @notice Add liquidity to an initialized pool
     * @dev Liquidity must have been provided beforehand
     */
    public AddLiquidityResult addLiquidity (AddLiquidityParams params) {
        PoolAddress.PoolKey poolKey = new PoolAddress.PoolKey(params.token0, params.token1, params.fee);

        Address pool = PoolAddress.getPool(this.factory, poolKey);
        Context.require(pool != null, "addLiquidity: pool doesn't exist");
        
        // compute the liquidity amount
        BigInteger sqrtPriceX96 = IConvexusPool.slot0(pool).sqrtPriceX96;
        BigInteger sqrtRatioAX96 = TickMath.getSqrtRatioAtTick(params.tickLower);
        BigInteger sqrtRatioBX96 = TickMath.getSqrtRatioAtTick(params.tickUpper);

        BigInteger liquidity = LiquidityAmounts.getLiquidityForAmounts(
            sqrtPriceX96,
            sqrtRatioAX96,
            sqrtRatioBX96,
            params.amount0Desired,
            params.amount1Desired
        );

        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.write(new MintCallbackData(poolKey, Context.getCaller()));

        PairAmounts amounts = IConvexusPool.mint (
            pool, 
            params.recipient, 
            params.tickLower, 
            params.tickUpper, 
            liquidity, 
            writer.toByteArray()
        );

        Context.require(
            amounts.amount0.compareTo(params.amount0Min) >= 0
        &&  amounts.amount1.compareTo(params.amount1Min) >= 0,
            "addLiquidity: Price slippage check"
        );

        return new AddLiquidityResult (liquidity, amounts.amount0, amounts.amount1, pool);
    }

    /**
     * @notice Add funds to the liquidity manager
     */
    // @External - this method is external through tokenFallback
    public void deposit (
        Address caller, 
        Address tokenIn, 
        BigInteger amountIn
    ) {
        // --- Checks ---
        Context.require(amountIn.compareTo(ZERO) > 0, 
            "deposit: Deposit amount cannot be less or equal to 0");

        // --- OK from here ---
        var depositedUser = this.deposited.at(caller);
        BigInteger oldBalance = depositedUser.getOrDefault(tokenIn, ZERO);
        depositedUser.set(tokenIn, oldBalance.add(amountIn));
        // Context.println("LM: deposit(" + caller + ")(" + IIRC2.symbol(tokenIn) + ") = " + this.deposited.at(caller).get(tokenIn));
    }

    /**
     * @notice Remove funds from the liquidity manager
     */
    public void withdraw (Address token) {
        final Address caller = Context.getCaller();

        var depositedUser = this.deposited.at(caller);
        BigInteger amount = depositedUser.getOrDefault(token, ZERO);

        if (amount.compareTo(ZERO) > 0) {
            IIRC2.transfer(token, caller, amount, JSONUtils.method("withdraw"));
            depositedUser.set(token, ZERO);
        }
    }

    // ================================================
    // Checks
    // ================================================
    private void checkEnoughDeposited (Address address, Address token, BigInteger amount) {
        var depositedUser = this.deposited.at(address);
        BigInteger userBalance = depositedUser.getOrDefault(token, ZERO);
        // Context.println("[Callee][checkEnoughDeposited][" + IIRC2.symbol(token) + "] " + userBalance + " / " + amount);
        Context.require(userBalance.compareTo(amount) >= 0,
            "checkEnoughDeposited: user didn't deposit enough funds");
    }

    // ================================================
    // Public variable getters
    // ================================================
    public BigInteger deposited (Address user, Address token) {
        return this.deposited.at(user).get(token);
    }
}
