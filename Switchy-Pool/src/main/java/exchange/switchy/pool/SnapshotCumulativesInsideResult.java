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

package exchange.switchy.pool;

import java.math.BigInteger;

public class SnapshotCumulativesInsideResult {
    BigInteger tickCumulativeInside;
    public BigInteger gettickCumulativeInside() { return this.tickCumulativeInside; }
    public void settickCumulativeInside(BigInteger v) { this.tickCumulativeInside = v; }

    BigInteger secondsPerLiquidityInsideX128;
    public BigInteger getsecondsPerLiquidityInsideX128() { return this.secondsPerLiquidityInsideX128; }
    public void setsecondsPerLiquidityInsideX128(BigInteger v) { this.secondsPerLiquidityInsideX128 = v; }

    BigInteger secondsInside;
    public BigInteger getsecondsInside() { return this.secondsInside; }
    public void setsecondsInside(BigInteger v) { this.secondsInside = v; }

    SnapshotCumulativesInsideResult (BigInteger tickCumulativeInside, BigInteger secondsPerLiquidityInsideX128, BigInteger secondsInside) {
      this.tickCumulativeInside = tickCumulativeInside;
      this.secondsPerLiquidityInsideX128 = secondsPerLiquidityInsideX128;
      this.secondsInside = secondsInside;
    }
}
