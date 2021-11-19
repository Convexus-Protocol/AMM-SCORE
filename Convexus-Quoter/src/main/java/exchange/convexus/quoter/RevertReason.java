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

package exchange.convexus.quoter;

import java.math.BigInteger;

public class RevertReason {
    public BigInteger amount;
    public BigInteger sqrtPriceX96After;
    public int tickAfter;
    public RevertReason (
        BigInteger amount,
        BigInteger sqrtPriceX96After,
        int tickAfter
    ) {
        this.amount = amount;
        this.sqrtPriceX96After = sqrtPriceX96After;
        this.tickAfter = tickAfter;
    }
}