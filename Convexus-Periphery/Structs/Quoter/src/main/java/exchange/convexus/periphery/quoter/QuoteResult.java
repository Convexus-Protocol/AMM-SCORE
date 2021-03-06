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
import java.util.Map;

public class QuoteResult {
    public BigInteger amountOut;
    public BigInteger sqrtPriceX96After;
    public int initializedTicksCrossed;

    public QuoteResult (
        BigInteger amountOut,
        BigInteger sqrtPriceX96After,
        int initializedTicksCrossed
    ) {
        this.amountOut = amountOut;
        this.sqrtPriceX96After = sqrtPriceX96After;
        this.initializedTicksCrossed = initializedTicksCrossed;
    }

    public static QuoteResult fromMap(Object call) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) call;
        return new QuoteResult (
            (BigInteger) map.get("amountOut"),
            (BigInteger) map.get("sqrtPriceX96After"),
            ((BigInteger) map.get("initializedTicksCrossed")).intValue()
        );
    }
}