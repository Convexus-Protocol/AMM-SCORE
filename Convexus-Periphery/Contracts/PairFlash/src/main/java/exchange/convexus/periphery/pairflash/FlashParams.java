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

package exchange.convexus.periphery.pairflash;

import java.math.BigInteger;

import score.Address;

//fee1 is the fee of the pool from the initial borrow
//fee2 is the fee of the first pool to arb from
//fee3 is the fee of the second pool to arb from
public class FlashParams {
    public Address token0;
    public Address token1;
    public int fee1;
    public BigInteger amount0;
    public BigInteger amount1;
    public int fee2;
    public int fee3;

    public FlashParams() {}

    public FlashParams (
      Address token0,
      Address token1,
      int fee1,
      BigInteger amount0,
      BigInteger amount1,
      int fee2,
      int fee3
    ) {
      this.token0 = token0;
      this.token1 = token1;
      this.fee1 = fee1;
      this.amount0 = amount0;
      this.amount1 = amount1;
      this.fee2 = fee2;
      this.fee3 = fee3;
    }
}