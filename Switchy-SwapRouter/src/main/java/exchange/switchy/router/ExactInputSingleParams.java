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

package exchange.switchy.router;

import java.math.BigInteger;

import score.Address;

public class ExactInputSingleParams {
    public Address tokenIn;
    public Address tokenOut;
    public int fee;
    public Address recipient;
    public BigInteger deadline;
    public BigInteger amountIn;
    public BigInteger amountOutMinimum;
    public BigInteger sqrtPriceLimitX96;

    public ExactInputSingleParams (
        Address tokenIn,
        Address tokenOut,
        int fee,
        Address recipient,
        BigInteger deadline,
        BigInteger amountIn,
        BigInteger amountOutMinimum,
        BigInteger sqrtPriceLimitX96
    ) {
        this.tokenIn = tokenIn;
        this.tokenOut = tokenOut;
        this.fee = fee;
        this.recipient = recipient;
        this.deadline = deadline;
        this.amountIn = amountIn;
        this.amountOutMinimum = amountOutMinimum;
        this.sqrtPriceLimitX96 = sqrtPriceLimitX96;
    }
}