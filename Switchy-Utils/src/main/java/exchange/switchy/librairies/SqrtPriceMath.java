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

import score.Context;

public class SqrtPriceMath {

  /**
   * @notice Helper that gets signed token0 delta
   * @param sqrtRatioAX96 A sqrt price
   * @param sqrtRatioBX96 Another sqrt price
   * @param liquidity The change in liquidity for which to compute the amount0 delta
   * @return amount0 Amount of token0 corresponding to the passed liquidityDelta between the two prices
   */
  public static BigInteger getAmount0Delta(
    BigInteger sqrtRatioAX96, 
    BigInteger sqrtRatioBX96,
    BigInteger liquidity
  ) {
    return liquidity.compareTo(BigInteger.ZERO) < 0
        ? getAmount0Delta(sqrtRatioAX96, sqrtRatioBX96, liquidity.negate(), false).negate()
        : getAmount0Delta(sqrtRatioAX96, sqrtRatioBX96, liquidity, true);
  }

  private static BigInteger getAmount0Delta(
    BigInteger sqrtRatioAX96, 
    BigInteger sqrtRatioBX96, 
    BigInteger liquidity,
    boolean roundUp
  ) {
    if (sqrtRatioAX96.compareTo(sqrtRatioBX96) > 0) {
      BigInteger tmp = sqrtRatioAX96;
      sqrtRatioAX96 = sqrtRatioBX96;
      sqrtRatioBX96 = tmp;
    }

    BigInteger numerator1 = liquidity.shiftLeft(FixedPoint96.RESOLUTION);
    BigInteger numerator2 = sqrtRatioBX96.subtract(sqrtRatioAX96);

    Context.require(sqrtRatioAX96.compareTo(BigInteger.ZERO) > 0);

    return
        roundUp
            ? UnsafeMath.divRoundingUp(
                FullMath.mulDivRoundingUp(numerator1, numerator2, sqrtRatioBX96),
                sqrtRatioAX96
            )
            : FullMath.mulDiv(numerator1, numerator2, sqrtRatioBX96).divide(sqrtRatioAX96);
  }

  public static BigInteger getAmount1Delta(
    BigInteger sqrtRatioAX96, 
    BigInteger sqrtRatioBX96,
    BigInteger liquidity
  ) {
    return liquidity.compareTo(BigInteger.ZERO) < 0
        ? getAmount1Delta(sqrtRatioAX96, sqrtRatioBX96, liquidity.negate(), false).negate()
        : getAmount1Delta(sqrtRatioAX96, sqrtRatioBX96, liquidity, true);
  }

  private static BigInteger getAmount1Delta(
    BigInteger sqrtRatioAX96, 
    BigInteger sqrtRatioBX96, 
    BigInteger liquidity,
    boolean roundUp
  ) {
    if (sqrtRatioAX96.compareTo(sqrtRatioBX96) > 0) {
      BigInteger tmp = sqrtRatioAX96;
      sqrtRatioAX96 = sqrtRatioBX96;
      sqrtRatioBX96 = tmp;
    }

    return
        roundUp
            ? FullMath.mulDivRoundingUp(liquidity, sqrtRatioBX96.subtract(sqrtRatioAX96), FixedPoint96.Q96)
            : FullMath.mulDiv(liquidity, sqrtRatioBX96.subtract(sqrtRatioAX96), FixedPoint96.Q96);
  }
}
