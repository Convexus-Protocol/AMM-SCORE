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

package exchange.convexus.periphery.quoter;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import exchange.convexus.core.librairies.OracleLib;
import exchange.convexus.pool.BeforeAfterObservation;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.ObserveResult;
import exchange.convexus.pool.Oracle;
import exchange.convexus.utils.MathUtils;
import score.Address;
import score.Context;

public class ObservationsReadOnly {
  // ================================================
  // Consts
  // ================================================
  // Contract class name
  private static final BigInteger TWO_POWER_32 = MathUtils.pow(BigInteger.TWO, 32);
  private final Address pool;

  // ================================================
  // DB Variables
  // ================================================

  // ================================================
  // Methods
  // ================================================
  public ObservationsReadOnly (Address pool) {
    this.pool = pool;
  }

  public Oracle.Observation get (int index) {
    return IConvexusPool.observations(this.pool, index);
  }

  private boolean lte (BigInteger time, BigInteger a, BigInteger b) {
    // if there hasn't been overflow, no need to adjust
    if (a.compareTo(time) <= 0 && b.compareTo(time) <= 0) {
      return a.compareTo(b) <= 0;
    }

    BigInteger aAdjusted = a.compareTo(time) > 0 ? a : a.add(TWO_POWER_32);
    BigInteger bAdjusted = b.compareTo(time) > 0 ? b : b.add(TWO_POWER_32);

    return aAdjusted.compareTo(bAdjusted) <= 0;
  }

  /**
   * @notice Fetches the observations beforeOrAt and atOrAfter a target, i.e. where [beforeOrAt, atOrAfter] is satisfied.
   * The result may be the same observation, or adjacent observations.
   * @dev The answer must be contained in the array, used when the target is located within the stored observation
   * boundaries: older than the most recent observation and younger, or the same age as, the oldest observation
   * @param time The current block.timestamp
   * @param target The timestamp at which the reserved observation should be for
   * @param index The index of the observation that was most recently written to the observations array
   * @param cardinality The number of populated elements in the oracle array
   * @return The observation recorded and after
   */
  private BeforeAfterObservation binarySearch (
    BigInteger time,
    BigInteger target,
    int index,
    int cardinality
  ) {
    int l = (index + 1) % cardinality; // oldest observation
    int r = l + cardinality - 1; // newest observation
    int i;

    Oracle.Observation beforeOrAt = null;
    Oracle.Observation atOrAfter = null;

    while (true) {
      i = (l + r) / 2;

      beforeOrAt = this.get((i + 1) % cardinality);

      // we've landed on an uninitialized tick, keep searching higher (more recently)
      if (!beforeOrAt.initialized) {
        l = i + 1;
        continue;
      }
      
      atOrAfter = this.get((i + 1) % cardinality);

      boolean targetAtOrAfter = lte(time, beforeOrAt.blockTimestamp, target);

      // check if we've found the answer!
      if (targetAtOrAfter && lte(time, target, atOrAfter.blockTimestamp)) {
        break;
      }

      if (!targetAtOrAfter) {
        r = i - 1;
      } else {
        l = i + 1;
      }
    }

    return new BeforeAfterObservation(beforeOrAt, atOrAfter);
  }

  private BeforeAfterObservation getSurroundingObservations (
    BigInteger time,
    BigInteger target,
    int tick,
    int index,
    BigInteger liquidity,
    int cardinality
  ) {
    // optimistically set before to the newest observation
    Oracle.Observation beforeOrAt = this.get(index);

    // if the target is chronologically at or after the newest observation, we can early return
    if (lte(time, beforeOrAt.blockTimestamp, target)) {
      if (beforeOrAt.blockTimestamp.equals(target)) {
        // if newest observation equals target, we're in the same block, so we can ignore atOrAfter
        Oracle.Observation atOrAfter = new Oracle.Observation(ZERO, ZERO, ZERO, false);
        return new BeforeAfterObservation(beforeOrAt, atOrAfter);
      } else {
        // otherwise, we need to transform
        return new BeforeAfterObservation(beforeOrAt, OracleLib.transform(beforeOrAt, target, tick, liquidity));
      }
    }

    // now, set before to the oldest observation
    beforeOrAt = this.get((index + 1) % cardinality);
    if (!beforeOrAt.initialized) {
      beforeOrAt = this.get(0);
    }

    // ensure that the target is chronologically at or after the oldest observation
    Context.require(lte(time, beforeOrAt.blockTimestamp, target), 
      "getSurroundingObservations: too old");

    // if we've reached this point, we have to binary search
    return binarySearch(time, target, index, cardinality);
  }

  public class ObserveSingleResult {
    public BigInteger tickCumulative;
    public BigInteger secondsPerLiquidityCumulativeX128;

    ObserveSingleResult (BigInteger tickCumulative, BigInteger secondsPerLiquidityCumulativeX128) {
      this.tickCumulative = tickCumulative;
      this.secondsPerLiquidityCumulativeX128 = secondsPerLiquidityCumulativeX128;
    }
  }

  public ObserveSingleResult observeSingle (BigInteger time, BigInteger secondsAgo, int tick, int index, BigInteger liquidity, int cardinality) {
    if (secondsAgo.equals(ZERO)) {
      Oracle.Observation last = this.get(index);
      if (!last.blockTimestamp.equals(time)) {
        last = OracleLib.transform(last, time, tick, liquidity);
        return new ObserveSingleResult(last.tickCumulative, last.secondsPerLiquidityCumulativeX128);
      }
    }

    BigInteger target = time.subtract(secondsAgo);

    BeforeAfterObservation result = getSurroundingObservations(time, target, tick, index, liquidity, cardinality);
    Oracle.Observation beforeOrAt = result.beforeOrAt;
    Oracle.Observation atOrAfter = result.atOrAfter;

    if (target.equals(beforeOrAt.blockTimestamp)) {
      // we're at the left boundary
      return new ObserveSingleResult(beforeOrAt.tickCumulative, beforeOrAt.secondsPerLiquidityCumulativeX128);
    } else if (target.equals(atOrAfter.blockTimestamp)) {
      // we're at the right boundary
      return new ObserveSingleResult(atOrAfter.tickCumulative, atOrAfter.secondsPerLiquidityCumulativeX128);
    } else {
      // we're in the middle
      BigInteger observationTimeDelta = atOrAfter.blockTimestamp.subtract(beforeOrAt.blockTimestamp);
      BigInteger targetDelta = target.subtract(beforeOrAt.blockTimestamp);
      return new ObserveSingleResult(
        beforeOrAt.tickCumulative.add(
          atOrAfter.tickCumulative.subtract(beforeOrAt.tickCumulative).divide(observationTimeDelta).multiply(targetDelta)
        ),
        beforeOrAt.secondsPerLiquidityCumulativeX128.add(
          atOrAfter.secondsPerLiquidityCumulativeX128.subtract(beforeOrAt.secondsPerLiquidityCumulativeX128).multiply(targetDelta).divide(observationTimeDelta)
        )
      );
    }
  }

  public ObserveResult observe (BigInteger time, BigInteger[] secondsAgos, int tick, int index, BigInteger liquidity, int cardinality) {
    Context.require(cardinality > 0,
      "observe: cardinality must be superior to 0");

    BigInteger[] tickCumulatives = new BigInteger[secondsAgos.length];
    BigInteger[] secondsPerLiquidityCumulativeX128s = new BigInteger[secondsAgos.length];

    for (int i = 0; i < secondsAgos.length; i++) {
      ObserveSingleResult result = observeSingle(time, secondsAgos[i], tick, index, liquidity, cardinality);
      tickCumulatives[i] = result.tickCumulative;
      secondsPerLiquidityCumulativeX128s[i] = result.secondsPerLiquidityCumulativeX128;
    }

    return new ObserveResult(tickCumulatives, secondsPerLiquidityCumulativeX128s);
  }

  public class WriteResult {
    public int indexUpdated;
    public int cardinalityUpdated;
    public WriteResult (int indexUpdated, int cardinalityUpdated) {
      this.indexUpdated = indexUpdated;
      this.cardinalityUpdated = cardinalityUpdated;
    }
  }

  public WriteResult write (
    int index, 
    BigInteger blockTimestamp, 
    int tick, 
    BigInteger liquidity,
    int cardinality, 
    int cardinalityNext
  ) {
    Oracle.Observation last = this.get(index);

    // early return if we've already written an observation this block
    if (last.blockTimestamp.equals(blockTimestamp)) {
      return new WriteResult(index, cardinality);
    }

    int cardinalityUpdated = cardinality;
    // if the conditions are right, we can bump the cardinality
    if (cardinalityNext > cardinality && index == (cardinality - 1)) {
        cardinalityUpdated = cardinalityNext;
    }

    int indexUpdated = (index + 1) % cardinalityUpdated;

    return new WriteResult(indexUpdated, cardinalityUpdated);
  }
}