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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import exchange.convexus.utils.MathUtils;
import score.Context;
import score.DictDB;
import score.ObjectReader;
import score.ObjectWriter;

public class Oracle {
    public static class Observation {
      // the block timestamp of the observation
      public BigInteger blockTimestamp;
      // the tick accumulator, i.e. tick * time elapsed since the pool was first initialized
      public BigInteger tickCumulative;
      // the seconds per liquidity, i.e. seconds elapsed / max(1, liquidity) since the pool was first initialized
      public BigInteger secondsPerLiquidityCumulativeX128;
      // whether or not the observation is initialized
      public Boolean initialized;

      public Observation (
        BigInteger blockTimestamp,
        BigInteger tickCumulative,
        BigInteger secondsPerLiquidityCumulativeX128,
        Boolean initialized
      ) {
        this.blockTimestamp = blockTimestamp;
        this.tickCumulative = tickCumulative;
        this.secondsPerLiquidityCumulativeX128 = secondsPerLiquidityCumulativeX128;
        this.initialized = initialized;
      }

      public static void writeObject(ObjectWriter w, Observation obj) {
        w.write(obj.blockTimestamp);
        w.write(obj.tickCumulative);
        w.write(obj.secondsPerLiquidityCumulativeX128);
        w.write(obj.initialized);
      }

      public static Observation readObject(ObjectReader r) {
        return new Observation(
          r.readBigInteger(), // blockTimestamp, 
          r.readBigInteger(), // tickCumulative, 
          r.readBigInteger(), // secondsPerLiquidityCumulativeX128, 
          r.readBoolean()     // initialized,
        );
      }
      
      public Observation transform (
        BigInteger blockTimestamp,
        int tick,
        BigInteger liquidity
      ) {
        Context.require(blockTimestamp.compareTo(this.blockTimestamp) >= 0,
          "transform: invalid blockTimestamp");
        BigInteger delta = blockTimestamp.subtract(this.blockTimestamp);
        BigInteger tickCumulative = this.tickCumulative.add(BigInteger.valueOf(tick).multiply(delta));
        BigInteger denominator = liquidity.compareTo(ZERO) > 0 ? liquidity : ONE;
        BigInteger secondsPerLiquidityCumulativeX128 = this.secondsPerLiquidityCumulativeX128.add(delta.shiftLeft(128).divide(denominator));
        return new Observation(blockTimestamp, tickCumulative, secondsPerLiquidityCumulativeX128, true);
      }
    }

    public static class Observations {
      // ================================================
      // Consts
      // ================================================
      // Contract class name
      private static final String NAME = "Observations";

      private static final BigInteger TWO_POWER_32 = MathUtils.pow(BigInteger.TWO, 32);
  
      // ================================================
      // DB Variables
      // ================================================
      // Returns data about a specific observation index
      private final DictDB<Integer, Oracle.Observation> observations = Context.newDictDB(NAME + "_observations", Oracle.Observation.class);
      private Oracle.Observation emptyObservation () {
        return new Oracle.Observation(ZERO, ZERO, ZERO, false);
      }

      // ================================================
      // Methods
      // ================================================
      public Observation get (int index) {
        return this.observations.getOrDefault(index, emptyObservation());
      }
      public void set (int index, Observation observation) {
        this.observations.set(index, observation);
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

      public class InitializeResult {
        public int cardinality;
        public int cardinalityNext;
        public InitializeResult (int cardinality, int cardinalityNext) {
          this.cardinality = cardinality;
          this.cardinalityNext = cardinalityNext;
        }
      }

      public InitializeResult initialize(BigInteger time) {
        Observation observation = new Observation(time, ZERO, ZERO, true);
        this.set(0, observation);
        return new InitializeResult(1, 1);
      }

      class BeforeAfterObservation {
        Observation beforeOrAt;
        Observation atOrAfter;

        BeforeAfterObservation (Observation beforeOrAt, Observation atOrAfter) {
          this.beforeOrAt = beforeOrAt;
          this.atOrAfter = atOrAfter;
        }
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

        Observation beforeOrAt = null;
        Observation atOrAfter = null;

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
        Observation beforeOrAt = this.get(index);

        // if the target is chronologically at or after the newest observation, we can early return
        if (lte(time, beforeOrAt.blockTimestamp, target)) {
          if (beforeOrAt.blockTimestamp.equals(target)) {
            // if newest observation equals target, we're in the same block, so we can ignore atOrAfter
            Observation atOrAfter = new Observation(ZERO, ZERO, ZERO, false);
            return new BeforeAfterObservation(beforeOrAt, atOrAfter);
          } else {
            // otherwise, we need to transform
            return new BeforeAfterObservation(beforeOrAt, beforeOrAt.transform(target, tick, liquidity));
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
          Observation last = this.get(index);
          if (!last.blockTimestamp.equals(time)) {
            last = last.transform(time, tick, liquidity);
            return new ObserveSingleResult(last.tickCumulative, last.secondsPerLiquidityCumulativeX128);
          }
        }

        BigInteger target = time.subtract(secondsAgo);

        BeforeAfterObservation result = getSurroundingObservations(time, target, tick, index, liquidity, cardinality);
        Observation beforeOrAt = result.beforeOrAt;
        Observation atOrAfter = result.atOrAfter;

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

      /**
       * @notice Prepares the oracle array to store up to `next` observations
       * @param self The stored oracle array
       * @param current The current next cardinality of the oracle array
       * @param next The proposed next cardinality which will be populated in the oracle array
       * @return next The next cardinality which will be populated in the oracle array
       */
      public int grow (int current, int next) {
        Context.require(current > 0, 
          "grow: current must be superior to 0");
        
          // no-op if the passed next value isn't greater than the current next value
          if (next <= current) {
            return current;
          }

          // this data will not be used because the initialized boolean is still false
          for (int i = current; i < next; i++) {
             Observation observation = this.get(i);
             observation.blockTimestamp = ONE;
             this.set(i, observation);
          }

          return next;
      }
      
      public class WriteResult {
        public int indexUpdated;
        public int cardinalityUpdated;
        public WriteResult (int indexUpdated, int cardinalityUpdated) {
          this.indexUpdated = indexUpdated;
          this.cardinalityUpdated = cardinalityUpdated;
        }
      }
      public WriteResult write(
        int index, 
        BigInteger blockTimestamp, 
        int tick, 
        BigInteger liquidity,
        int cardinality, 
        int cardinalityNext
      ) {
        Observation last = this.get(index);
        
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
        this.set(indexUpdated, last.transform(blockTimestamp, tick, liquidity));

        return new WriteResult(indexUpdated, cardinalityUpdated);
      }
    }
}