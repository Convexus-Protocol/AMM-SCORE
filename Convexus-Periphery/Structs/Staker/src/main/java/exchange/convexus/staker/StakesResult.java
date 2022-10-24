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

package exchange.convexus.staker;

import java.math.BigInteger;
import java.util.Map;

public class StakesResult {
  public BigInteger secondsPerLiquidityInsideInitialX128;
  public BigInteger liquidity;

  public StakesResult () {}

  public StakesResult (
    BigInteger secondsPerLiquidityInsideInitialX128,
    BigInteger liquidity
  ) {
    this.secondsPerLiquidityInsideInitialX128 = secondsPerLiquidityInsideInitialX128;
    this.liquidity = liquidity;
  }

  public static StakesResult fromMap(Object call) {
    @SuppressWarnings("unchecked")
    Map<String,Object> map = (Map<String,Object>) call;
    return new StakesResult(
        (BigInteger) map.get("secondsPerLiquidityInsideInitialX128"),
        (BigInteger) map.get("liquidity")
    );
  }
}
