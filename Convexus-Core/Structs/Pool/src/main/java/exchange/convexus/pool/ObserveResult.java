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

package exchange.convexus.pool;

import java.math.BigInteger;
import java.util.Map;

public class ObserveResult {
  // Cumulative tick values as of each `secondsAgos` from the current block timestamp
  public BigInteger[] tickCumulatives;
  // Cumulative seconds per liquidity-in-range value as of each `secondsAgos` from the current block
  public BigInteger[] secondsPerLiquidityCumulativeX128s;

  public ObserveResult (BigInteger[] tickCumulatives, BigInteger[] secondsPerLiquidityCumulativeX128s) {
    this.tickCumulatives = tickCumulatives;
    this.secondsPerLiquidityCumulativeX128s = secondsPerLiquidityCumulativeX128s;
  }

  public static ObserveResult fromMap (Object call) {
    @SuppressWarnings("unchecked")
    Map<String,Object> map = (Map<String,Object>) call;
    return new ObserveResult (
      (BigInteger[]) map.get("tickCumulatives"), 
      (BigInteger[]) map.get("secondsPerLiquidityCumulativeX128s")
    );
  }
}