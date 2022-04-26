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

package exchange.convexus.periphery.staker;

import java.math.BigInteger;

import exchange.convexus.librairies.FullMath;
import exchange.convexus.staker.RewardAmount;
import exchange.convexus.utils.MathUtils;
import score.Context;

public class RewardMath {
  /// @notice Compute the amount of rewards owed given parameters of the incentive and stake
  /// @param totalRewardUnclaimed The total amount of unclaimed rewards left for an incentive
  /// @param totalSecondsClaimedX128 How many full liquidity-seconds have been already claimed for the incentive
  /// @param startTime When the incentive rewards began in epoch seconds
  /// @param endTime When rewards are no longer being dripped out in epoch seconds
  /// @param liquidity The amount of liquidity, assumed to be constant over the period over which the snapshots are measured
  /// @param secondsPerLiquidityInsideInitialX128 The seconds per liquidity of the liquidity tick range as of the beginning of the period
  /// @param secondsPerLiquidityInsideX128 The seconds per liquidity of the liquidity tick range as of the current block timestamp
  /// @param currentTime The current block timestamp, which must be greater than or equal to the start time
  /// @return reward The amount of rewards owed
  /// @return secondsInsideX128 The total liquidity seconds inside the position's range for the duration of the stake
  public static RewardAmount computeRewardAmount(
    BigInteger totalRewardUnclaimed, 
    BigInteger totalSecondsClaimedX128,
    BigInteger startTime, 
    BigInteger endTime, 
    BigInteger liquidity, 
    BigInteger secondsPerLiquidityInsideInitialX128,
    BigInteger secondsPerLiquidityInsideX128, 
    BigInteger currentTime
  ) {
    // this should never be called before the start time
    Context.require(currentTime.compareTo(startTime) >= 0);

    // this operation is safe, as the difference cannot be greater than 1/stake.liquidity
    BigInteger secondsInsideX128 = secondsPerLiquidityInsideX128.subtract(secondsPerLiquidityInsideInitialX128).multiply(liquidity);

    BigInteger totalSecondsUnclaimedX128 =
        ((MathUtils.max(endTime, currentTime).subtract(startTime)).shiftLeft(128)).subtract(totalSecondsClaimedX128);

    BigInteger reward = FullMath.mulDiv(totalRewardUnclaimed, secondsInsideX128, totalSecondsUnclaimedX128);

    return new RewardAmount (reward, secondsInsideX128);
  }
}
