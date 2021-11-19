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

package exchange.convexus.librairies;

import java.math.BigInteger;
import java.util.Map;

public class ObserveResult {
  // Cumulative tick values as of each `secondsAgos` from the current block timestamp
  public BigInteger[] tickCumulatives;
  BigInteger[] gettickCumulatives () { return this.tickCumulatives; }
  void settickCumulatives (BigInteger[] v) { this.tickCumulatives = v; }

  // Cumulative seconds per liquidity-in-range value as of each `secondsAgos` from the current block
  public BigInteger[] secondsPerLiquidityCumulativeX128s;
  BigInteger[] getsecondsPerLiquidityCumulativeX128s () { return this.secondsPerLiquidityCumulativeX128s; }
  void setsecondsPerLiquidityCumulativeX128s (BigInteger[] v) { this.secondsPerLiquidityCumulativeX128s = v; }

  ObserveResult (BigInteger[] tickCumulatives, BigInteger[] secondsPerLiquidityCumulativeX128s) {
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