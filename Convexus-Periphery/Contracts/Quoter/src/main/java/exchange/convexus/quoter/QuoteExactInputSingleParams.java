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

package exchange.convexus.quoter;

import java.math.BigInteger;

import score.Address;

public class QuoteExactInputSingleParams {
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