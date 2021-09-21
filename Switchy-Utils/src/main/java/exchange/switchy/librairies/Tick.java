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

package exchange.switchy.librairies;

import java.math.BigInteger;

public class Tick {
    public static class Info {
      // the total position liquidity that references this tick
      public BigInteger liquidityGross;
      public BigInteger getliquidityGross() { return this.liquidityGross; }
      public void setliquidityGross(BigInteger v) { this.liquidityGross = v; }

      // amount of net liquidity added (subtracted) when tick is crossed from left to right (right to left),
      public BigInteger liquidityNet;
      public BigInteger getliquidityNet() { return this.liquidityNet; }
      public void setliquidityNet(BigInteger v) { this.liquidityNet = v; }

      // fee growth per unit of liquidity on the _other_ side of this tick (relative to the current tick)
      // only has relative meaning, not absolute — the value depends on when the tick is initialized
      public BigInteger feeGrowthOutside0X128;
      public BigInteger getfeeGrowthOutside0X128() { return this.feeGrowthOutside0X128; }
      public void setfeeGrowthOutside0X128(BigInteger v) { this.feeGrowthOutside0X128 = v; }

      public BigInteger feeGrowthOutside1X128;
      public BigInteger getfeeGrowthOutside1X128() { return this.feeGrowthOutside1X128; }
      public void setfeeGrowthOutside1X128(BigInteger v) { this.feeGrowthOutside1X128 = v; }

      // the cumulative tick value on the other side of the tick
      public BigInteger tickCumulativeOutside;
      public BigInteger gettickCumulativeOutside() { return this.tickCumulativeOutside; }
      public void settickCumulativeOutside(BigInteger v) { this.tickCumulativeOutside = v; }

      // the seconds per unit of liquidity on the _other_ side of this tick (relative to the current tick)
      // only has relative meaning, not absolute — the value depends on when the tick is initialized
      public BigInteger secondsPerLiquidityOutsideX128;
      public BigInteger getsecondsPerLiquidityOutsideX128() { return this.secondsPerLiquidityOutsideX128; }
      public void setsecondsPerLiquidityOutsideX128(BigInteger v) { this.secondsPerLiquidityOutsideX128 = v; }

      // the seconds spent on the other side of the tick (relative to the current tick)
      // only has relative meaning, not absolute — the value depends on when the tick is initialized
      public BigInteger secondsOutside;
      public BigInteger getsecondsOutside() { return this.secondsOutside; }
      public void setsecondsOutside(BigInteger v) { this.secondsOutside = v; }

      // true if the tick is initialized, i.e. the value is exactly equivalent to the expression liquidityGross != 0
      // these 8 bits are set to prevent fresh sstores when crossing newly initialized ticks
      // Outside values can only be used if the tick is initialized, i.e. if liquidityGross is greater than 0.
      // In addition, these values are only relative and must be used only in comparison to previous snapshots for
      // a specific position.
      public boolean initialized;
      public boolean getinitialized() { return this.initialized; }
      public void setinitialized(boolean v) { this.initialized = v; }

      public Info () {
        
      }
    }

    public static BigInteger tickSpacingToMaxLiquidityPerTick (int tickSpacing) {
      int minTick = (int) ((float) TickMath.MIN_TICK / tickSpacing) * tickSpacing;
      int maxTick = (int) ((float) TickMath.MAX_TICK / tickSpacing) * tickSpacing;
      int numTicks = ((maxTick - minTick) / tickSpacing) + 1;
      return new BigInteger("340282366920938463463374607431768211456").divide(BigInteger.valueOf(numTicks));
    }
}
