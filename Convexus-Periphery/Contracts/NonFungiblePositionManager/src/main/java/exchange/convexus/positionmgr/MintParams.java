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

package exchange.convexus.positionmgr;

import java.math.BigInteger;

import score.Address;

public class MintParams {
    // The first token of a pool, unsorted
    public Address token0;
    // The second token of a pool, unsorted
    public Address token1;
    // The fee level of the pool
    public int fee;
    // The lower tick of the position
    public int tickLower;
    // The upper tick of the position
    public int tickUpper;
    // The desired amount of token0 to be spent,
    public BigInteger amount0Desired;
    // The desired amount of token1 to be spent,
    public BigInteger amount1Desired;
    // The minimum amount of token0 to spend, which serves as a slippage check,
    public BigInteger amount0Min;
    // The minimum amount of token1 to spend, which serves as a slippage check,
    public BigInteger amount1Min;
    // The address that received the output of the swap
    public Address recipient;
    // The unix time after which a mint will fail, to protect against long-pending transactions and wild swings in prices
    public BigInteger deadline;

    public MintParams() {}
}