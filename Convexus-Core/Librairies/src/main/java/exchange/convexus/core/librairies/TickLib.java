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

package exchange.convexus.core.librairies;

import java.math.BigInteger;
import exchange.convexus.librairies.TickMath;

public class TickLib {
  public static BigInteger tickSpacingToMaxLiquidityPerTick (int tickSpacing) {
    int minTick = ((int) ((float) TickMath.MIN_TICK / tickSpacing)) * tickSpacing;
    int maxTick = ((int) ((float) TickMath.MAX_TICK / tickSpacing)) * tickSpacing;
    int numTicks = ((maxTick - minTick) / tickSpacing) + 1;
    // 340282366920938463463374607431768211456 = 2^128
    return new BigInteger("340282366920938463463374607431768211456").divide(BigInteger.valueOf(numTicks));
  }
}
