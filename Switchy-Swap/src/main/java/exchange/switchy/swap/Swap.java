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

package exchange.switchy.swap;


import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;
import scorex.io.Reader;
import scorex.io.StringReader;
import exchange.switchy.router.ExactInputParams;
import exchange.switchy.router.ExactInputSingleParams;
import exchange.switchy.router.ExactOutputParams;
import exchange.switchy.router.ExactOutputSingleParams;
import exchange.switchy.utils.BytesUtils;
import exchange.switchy.utils.ReentrancyLock;
import exchange.switchy.utils.StringUtils;
import exchange.switchy.utils.TimeUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * @title Swap contract implementation
 * @notice An example contract using the Switchy swap function
 */
public class Swap {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    private static final String NAME = "Swap";

    // Contract name
    private final String name;
    private final Address swapRouter;

    // For this example, we will set the pool fee to 0.3%.
    public final int poolFee = 3000;

    // This example swaps IUSDC/sICX for single path swaps and IUSDC/bnUSD/sICX for multi path swaps.
    public final Address IUSDC = Address.fromString("cx6b175474e89094c44da98b954eedeac495271d0f");
    public final Address SICX  = Address.fromString("cxc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2");
    public final Address BNUSD = Address.fromString("cxa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48");

    // ================================================
    // DB Variables
    // ================================================
    private final ReentrancyLock reentreancy = new ReentrancyLock(NAME + "_reentreancy");

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public Swap(
        Address _swapRouter
    ) {
        this.name = "Switchy IUSDC-BNUSD-SICX Swap";
        this.swapRouter = _swapRouter;
    }

    /**
     * @notice swapExactInputSingle swaps a fixed amount of IUSDC for a maximum possible amount of sICX
     * using the IUSDC/sICX 0.3% pool by calling `exactInputSingle` in the swap router.
     * @dev The calling address must approve this contract to spend at least `amountIn` worth of its IUSDC for this function to succeed.
     * @param amountIn The exact amount of IUSDC that will be swapped for sICX.
     * @return amountOut The amount of sICX received.
     */
    // @External - this method is external through tokenFallback
    private void swapExactInputSingle (Address caller, Address tokenIn, BigInteger amountIn) {
        reentreancy.lock(true);

        // Naively set amountOutMinimum to 0. In production, use an oracle or other data source to choose a safer value for amountOutMinimum.
        // We also set the sqrtPriceLimitx96 to be 0 to ensure we swap our exact input amount.
        ExactInputSingleParams params = new ExactInputSingleParams(
            SICX,
            poolFee,
            caller,
            TimeUtils.nowSeconds(),
            ZERO,
            ZERO
        );

        // Forward tokenIn to the router and call the "exactInputSingle" method
        JsonObject data = Json.object()
            .add("method", "exactInputSingle")
            .add("params", params.toJson());

        // The call to `exactInputSingle` executes the swap.
        Context.call(tokenIn, "transfer", this.swapRouter, amountIn, data.toString().getBytes());

        reentreancy.lock(false);
    }


    /**
     * @notice swapExactOutputSingle swaps a minimum possible amount of IUSDC for a fixed amount of SICX.
     * @param amountOut The exact amount of SICX to receive from the swap.
     * @param amountInMaximum The amount of IUSDC we are willing to spend to receive the specified amount of SICX.
     * @return amountIn The amount of IUSDC actually spent in the swap.
     */
    // @External - this method is external through tokenFallback
    private void swapExactOutputSingle(
        Address caller, 
        Address tokenIn, 
        BigInteger amountOut,
        BigInteger amountInMaximum
    ) {
        reentreancy.lock(true);

        // Approve the router to spend the specifed `amountInMaximum` of DAI.
        // In production, you should choose the maximum amount to spend based on oracles or other data sources to acheive a better swap.
        ExactOutputSingleParams params = new ExactOutputSingleParams(
            this.SICX,
            poolFee,
            caller,
            TimeUtils.nowSeconds(),
            amountOut,
            ZERO
        );

        // Forward IUSDC to the router and call the "exactOutputSingle" method
        JsonObject data = Json.object()
            .add("method", "exactOutputSingle")
            .add("params", params.toJson());

        // Executes the swap returning the token exceess not needed to spend to receive the desired amountOut.
        Context.call(tokenIn, "transfer", this.swapRouter, amountInMaximum, data.toString().getBytes());

        BigInteger excess = (BigInteger) Context.call(tokenIn, "balanceOf", Context.getAddress());
        // send back the tokens excess to the caller if there's any
        if (excess.compareTo(ZERO) > 0) {
            Context.call(tokenIn, "transfer", caller, excess, "excess".getBytes());
        }

        reentreancy.lock(false);
    }


    // @External - this method is external through tokenFallback
    private void swapExactInputMultihop(Address caller, Address tokenIn, BigInteger amountIn) {
        reentreancy.lock(true);

        // Multiple pool swaps are encoded through bytes called a `path`. A path is a sequence of token addresses and poolFees that define the pools used in the swaps.
        // The format for pool encoding is (tokenIn, fee, tokenOut/tokenIn, fee, tokenOut) where tokenIn/tokenOut parameter is the shared token across the pools.
        // Since we are swapping USDC to BNUSD and then BNUSD to SICX the path encoding is (USDC, 0.3%, BNUSD, 0.3%, SICX).
        ExactInputParams params = new ExactInputParams(
            BytesUtils.concat(
                tokenIn.toByteArray(),
                BytesUtils.intToBytes(poolFee),
                BNUSD.toByteArray(),
                BytesUtils.intToBytes(poolFee),
                SICX.toByteArray()
            ),
            caller,
            TimeUtils.nowSeconds(),
            ZERO
        );

        // Forward tokenIn to the router and call the "exactInput" method
        JsonObject data = Json.object()
            .add("method", "exactInput")
            .add("params", params.toJson());

        // Executes the swap.
        Context.call(tokenIn, "transfer", this.swapRouter, amountIn, data.toString().getBytes());

        reentreancy.lock(false);
    }

    
    /**
     * @notice swapExactOutputMultihop swaps a minimum possible amount of IUSDC for a fixed amount of WETH through an intermediary pool.
     * For this example, we want to swap IUSDC for SICX through a BNUSD pool but we specify the desired amountOut of SICX. Notice how the path encoding is slightly different in for exact output swaps.
     * @dev The calling address must approve this contract to spend its IUSDC for this function to succeed. As the amount of input IUSDC is variable,
     * the calling address will need to approve for a slightly higher amount, anticipating some variance.
     * @param amountOut The desired amount of SICX.
     * @param amountInMaximum The maximum amount of IUSDC willing to be swapped for the specified amountOut of SICX.
     * @return amountIn The amountIn of IUSDC actually spent to receive the desired amountOut.
     */
    // @External - this method is external through tokenFallback
    private void swapExactOutputMultihop(
        Address caller, 
        Address tokenIn,
        BigInteger amountOut,
        BigInteger amountInMaximum
    ) {
        reentreancy.lock(true);

        // The parameter path is encoded as (tokenOut, fee, tokenIn/tokenOut, fee, tokenIn)
        // The tokenIn/tokenOut field is the shared token between the two pools used in the multiple pool swap. In this case BNUSD is the "shared" token.
        // For an exactOutput swap, the first swap that occurs is the swap which returns the eventual desired token.
        // In this case, our desired output token is WETH9 so that swap happpens first, and is encoded in the path accordingly.
        ExactOutputParams params = new ExactOutputParams(
            BytesUtils.concat(
                SICX.toByteArray(),
                BytesUtils.intToBytes(poolFee),
                BNUSD.toByteArray(),
                BytesUtils.intToBytes(poolFee),
                IUSDC.toByteArray()
            ), 
            caller,
            TimeUtils.nowSeconds(),
            amountOut
        );

        // Forward IUSDC to the router and call the "exactOutput" method
        JsonObject data = Json.object()
            .add("method", "exactOutput")
            .add("params", params.toJson());

        // Executes the swap returning the token exceess not needed to spend to receive the desired amountOut.
        Context.call(tokenIn, "transfer", this.swapRouter, amountInMaximum, data.toString().getBytes());

        BigInteger excess = (BigInteger) Context.call(tokenIn, "balanceOf", Context.getAddress());
        // send back the tokens excess to the caller if there's any
        if (excess.compareTo(ZERO) > 0) {
            Context.call(tokenIn, "transfer", caller, excess, "excess".getBytes());
        }

        reentreancy.lock(false);
    }

    @External
    public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) throws Exception {
        Reader reader = new StringReader(new String(_data));
        JsonValue input = Json.parse(reader);
        JsonObject root = input.asObject();
        String method = root.get("method").asString();
        Address token = Context.getCaller();

        // Ensure we received IUSDC as input
        Context.require(token.equals(IUSDC));

        switch (method)
        {
            case "swapExactInputSingle": {
                swapExactInputSingle(_from, token, _value);
                break;
            }

            case "swapExactInputMultihop": {
                swapExactInputMultihop(_from, token, _value);
                break;
            }

            case "swapExactOutputSingle": {
                JsonObject params = root.get("params").asObject();
                BigInteger amountOut = StringUtils.toBigInt(params.get("amountOut").asString());
                BigInteger amountInMaximum = _value;
                swapExactOutputSingle(_from, token, amountOut, amountInMaximum);
                break;
            }

            case "swapExactOutputMultihop": {
                JsonObject params = root.get("params").asObject();
                BigInteger amountOut = StringUtils.toBigInt(params.get("amountOut").asString());
                BigInteger amountInMaximum = _value;
                swapExactOutputMultihop(_from, token, amountOut, amountInMaximum);
                break;
            }

            default:
                Context.revert("tokenFallback: Unimplemented tokenFallback action");
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
