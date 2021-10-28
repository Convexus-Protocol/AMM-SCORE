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

package exchange.convexus.liquidity;

import java.math.BigInteger;

import score.Address;

public class AddLiquidityParams {
    public Address token0;
    public Address token1;
    public int fee;
    public Address recipient;
    public int tickLower;
    public int tickUpper;
    public BigInteger amount0Desired;
    public BigInteger amount1Desired;
    public BigInteger amount0Min;
    public BigInteger amount1Min;

    public AddLiquidityParams () {}
}