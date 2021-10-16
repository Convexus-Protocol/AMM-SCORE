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

package exchange.convexus.pool;

import java.math.BigInteger;

import exchange.convexus.librairies.FullMath;
import exchange.convexus.librairies.SqrtPriceMath;

class ComputeSwapStepResult {
  public BigInteger sqrtRatioNextX96;
  public BigInteger amountIn;
  public BigInteger amountOut;
  public BigInteger feeAmount;
  public ComputeSwapStepResult (
    BigInteger sqrtRatioNextX96,
    BigInteger amountIn,
    BigInteger amountOut,
    BigInteger feeAmount
  ) {
    this.sqrtRatioNextX96 = sqrtRatioNextX96;
    this.amountIn = amountIn;
    this.amountOut = amountOut;
    this.feeAmount = feeAmount;
  }
}

public class SwapMath {

  /**
   * @notice Computes the result of swapping some amount in, or amount out, given the parameters of the swap
   * @dev The fee, plus the amount in, will never exceed the amount remaining if the swap's `amountSpecified` is positive
   * @param sqrtRatioCurrentX96 The current sqrt price of the pool
   * @param sqrtRatioTargetX96 The price that cannot be exceeded, from which the direction of the swap is inferred
   * @param liquidity The usable liquidity
   * @param amountRemaining How much input or output amount is remaining to be swapped in/out
   * @param feePips The fee taken from the input amount, expressed in hundredths of a bip
   * @return sqrtRatioNextX96 The price after swapping the amount in/out, not to exceed the price target
   * @return amountIn The amount to be swapped in, of either token0 or token1, based on the direction of the swap
   * @return amountOut The amount to be received, of either token0 or token1, based on the direction of the swap
   * @return feeAmount The amount of input that will be taken as a fee
   */
  public static ComputeSwapStepResult computeSwapStep(
    BigInteger sqrtRatioCurrentX96, 
    BigInteger sqrtRatioTargetX96, 
    BigInteger liquidity,
    BigInteger amountRemaining, 
    int feePips
  ) {
    boolean zeroForOne = sqrtRatioCurrentX96.compareTo(sqrtRatioTargetX96) >= 0;
    boolean exactIn = amountRemaining.compareTo(BigInteger.ZERO) >= 0;
    final BigInteger TEN_E6 = BigInteger.valueOf(1000000);
    
    BigInteger sqrtRatioNextX96 = BigInteger.ZERO;
    BigInteger amountIn = BigInteger.ZERO;
    BigInteger amountOut = BigInteger.ZERO;
    BigInteger feeAmount = BigInteger.ZERO;
    
    if (exactIn) {
      BigInteger amountRemainingLessFee = FullMath.mulDiv(amountRemaining, TEN_E6.subtract(BigInteger.valueOf(feePips)), TEN_E6);
      amountIn = zeroForOne
          ? SqrtPriceMath.getAmount0Delta(sqrtRatioTargetX96, sqrtRatioCurrentX96, liquidity, true)
          : SqrtPriceMath.getAmount1Delta(sqrtRatioCurrentX96, sqrtRatioTargetX96, liquidity, true);

      if (amountRemainingLessFee.compareTo(amountIn) >= 0) {
        sqrtRatioNextX96 = sqrtRatioTargetX96;
      }
      else {
        sqrtRatioNextX96 = SqrtPriceMath.getNextSqrtPriceFromInput(
          sqrtRatioCurrentX96,
          liquidity,
          amountRemainingLessFee,
          zeroForOne
        );
      }
  } else {
    amountOut = zeroForOne
          ? SqrtPriceMath.getAmount1Delta(sqrtRatioTargetX96, sqrtRatioCurrentX96, liquidity, false)
          : SqrtPriceMath.getAmount0Delta(sqrtRatioCurrentX96, sqrtRatioTargetX96, liquidity, false);

      if (amountRemaining.negate().compareTo(amountOut) >= 0) {
        sqrtRatioNextX96 = sqrtRatioTargetX96;
      }
      else {
        sqrtRatioNextX96 = SqrtPriceMath.getNextSqrtPriceFromOutput(
          sqrtRatioCurrentX96,
          liquidity,
          amountRemaining.negate(),
          zeroForOne
        );
      }
    }

    return new ComputeSwapStepResult(sqrtRatioNextX96, amountIn, amountOut, feeAmount);
  }
}
