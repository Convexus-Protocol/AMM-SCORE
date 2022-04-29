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

package exchange.convexus.periphery.poolreadonly;

import java.math.BigInteger;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.Tick;
import score.Address;

public class TicksReadOnly {
  
  // ================================================
  // Consts
  // ================================================
  // Class name
  private final Address pool;
  
  // ================================================
  // DB Variables
  // ================================================

  // ================================================
  // Methods
  // ================================================
  public TicksReadOnly (Address pool) {
    this.pool = pool;
  }

  public Tick.Info get (int key) {
    var result = IConvexusPool.ticks(this.pool, key);
    return result == null ? Tick.Info.empty() : result;
  }

  /**
   * @notice Transitions to next tick as needed by price movement
   * @param tick The destination tick of the transition
   * @param feeGrowthGlobal0X128 The all-time global fee growth, per unit of liquidity, in token0
   * @param feeGrowthGlobal1X128 The all-time global fee growth, per unit of liquidity, in token1
   * @param secondsPerLiquidityCumulativeX128 The current seconds per liquidity
   * @param tickCumulative The tick * time elapsed since the pool was first initialized
   * @param time The current block.timestamp
   * @return liquidityNet The amount of liquidity added (subtracted) when tick is crossed from left to right (right to left)
   */
  public BigInteger cross (
    int tick, 
    BigInteger feeGrowthGlobal0X128, 
    BigInteger feeGrowthGlobal1X128, 
    BigInteger secondsPerLiquidityCumulativeX128,
    BigInteger tickCumulative, 
    BigInteger time
  ) {
    Tick.Info info = this.get(tick);
    info.feeGrowthOutside0X128 = feeGrowthGlobal0X128.subtract(info.feeGrowthOutside0X128);
    info.feeGrowthOutside1X128 = feeGrowthGlobal1X128.subtract(info.feeGrowthOutside1X128);
    info.secondsPerLiquidityOutsideX128 = secondsPerLiquidityCumulativeX128.subtract(info.secondsPerLiquidityOutsideX128);
    info.tickCumulativeOutside = tickCumulative.subtract(info.tickCumulativeOutside);
    info.secondsOutside = time.subtract(info.secondsOutside);
    return info.liquidityNet;
  }
}
