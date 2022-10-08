/*
 * Copyright 2022 Convexus Protocol
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

package exchange.convexus.periphery.quoter;

import java.math.BigInteger;

public class QuoteMultiResult {
    // The amount of the last token that would be received
    public BigInteger amountOut;
    // List of the sqrt price after the swap for each pool in the path
    public BigInteger[] sqrtPriceX96AfterList;
    // List of the initialized ticks that the swap crossed for each pool in the path
    public int[] initializedTicksCrossedList;

    public QuoteMultiResult(
        BigInteger amountOut,
        BigInteger[] sqrtPriceX96AfterList,
        int[] initializedTicksCrossedList
    ) {
        this.amountOut = amountOut;
        this.sqrtPriceX96AfterList = sqrtPriceX96AfterList;
        this.initializedTicksCrossedList = initializedTicksCrossedList;
    }
}