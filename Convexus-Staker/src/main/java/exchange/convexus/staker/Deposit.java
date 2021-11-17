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

package exchange.convexus.staker;

import java.math.BigInteger;

import score.Address;

/// @notice Represents the deposit of a liquidity NFT
public class Deposit {
    public Address owner;
    public BigInteger numberOfStakes;
    public int tickLower;
    public int tickUpper;

    public Deposit () {}

    public Deposit (
        Address owner,
        BigInteger numberOfStakes,
        int tickLower,
        int tickUpper
    ) {
        this.owner = owner;
        this.numberOfStakes = numberOfStakes;
        this.tickLower = tickLower;
        this.tickUpper = tickUpper;
    }
}