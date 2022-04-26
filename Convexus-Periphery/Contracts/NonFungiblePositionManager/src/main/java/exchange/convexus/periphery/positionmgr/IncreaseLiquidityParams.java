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

package exchange.convexus.periphery.positionmgr;

import java.math.BigInteger;

public class IncreaseLiquidityParams {
    public BigInteger tokenId;
    public BigInteger amount0Desired;
    public BigInteger amount1Desired;
    public BigInteger amount0Min;
    public BigInteger amount1Min;
    public BigInteger deadline;

    public IncreaseLiquidityParams () {}
    
    public IncreaseLiquidityParams (
        BigInteger tokenId,
        BigInteger amount0Desired,
        BigInteger amount1Desired,
        BigInteger amount0Min,
        BigInteger amount1Min,
        BigInteger deadline
    ) {
        this.tokenId = tokenId;
        this.amount0Desired = amount0Desired;
        this.amount1Desired = amount1Desired;
        this.amount0Min = amount0Min;
        this.amount1Min = amount1Min;
        this.deadline = deadline;
    }
}