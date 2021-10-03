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
import exchange.switchy.router.ExactInputSingleParams;
import exchange.switchy.utils.ReentrancyLock;
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

    // This example swaps IUSDC/wICX for single path swaps and IUSDC/bnUSD/wICX for multi path swaps.
    public final Address IUSDC = Address.fromString("cx6b175474e89094c44da98b954eedeac495271d0f");
    public final Address WICX  = Address.fromString("cxc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2");
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
        this.name = "Switchy IUSDC-BNUSD-WICX Swap";
        this.swapRouter = _swapRouter;
    }

    /**
     * @notice swapExactInputSingle swaps a fixed amount of IUSDC for a maximum possible amount of wICX
     * using the IUSDC/wICX 0.3% pool by calling `exactInputSingle` in the swap router.
     * @dev The calling address must approve this contract to spend at least `amountIn` worth of its IUSDC for this function to succeed.
     * @param amountIn The exact amount of IUSDC that will be swapped for wICX.
     * @return amountOut The amount of wICX received.
     */
    // @External - this method is external through tokenFallback
    private BigInteger swapExactInputSingle (Address caller, Address token, BigInteger amountIn) {
        reentreancy.lock(true);

        // Ensure we received IUSDC
        Context.require(token.equals(IUSDC));

        // Naively set amountOutMinimum to 0. In production, use an oracle or other data source to choose a safer value for amountOutMinimum.
        // We also set the sqrtPriceLimitx96 to be 0 to ensure we swap our exact input amount.
        ExactInputSingleParams params = new ExactInputSingleParams(
            WICX,
            poolFee,
            caller,
            TimeUtils.nowSeconds(),
            ZERO,
            ZERO
        );

        // Forward IUSDC to the router and call the "exactInputSingle" method
        JsonObject data = Json.object().add("method", "exactInputSingle");
        // The call to `exactInputSingle` executes the swap.
        Context.call(IUSDC, "transfer", this.swapRouter, amountIn, data.toString().getBytes());
        
        reentreancy.lock(false);
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
