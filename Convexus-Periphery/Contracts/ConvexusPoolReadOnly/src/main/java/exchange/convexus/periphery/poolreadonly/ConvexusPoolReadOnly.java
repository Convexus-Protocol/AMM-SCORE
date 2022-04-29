/*
 * Copyright 2022 Convexus Protocol
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package exchange.convexus.periphery.poolreadonly;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import exchange.convexus.core.interfaces.poolcallee.IConvexusPoolCalleeReadOnly;
import exchange.convexus.core.librairies.LiquidityMath;
import exchange.convexus.core.librairies.SwapMath;
import exchange.convexus.librairies.FixedPoint128;
import exchange.convexus.librairies.FullMath;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.Slot0;
import exchange.convexus.pool.StepComputations;
import exchange.convexus.pool.SwapCache;
import exchange.convexus.pool.SwapState;
import exchange.convexus.utils.TimeUtils;
import score.Address;
import score.Context;
import score.annotation.External;

public class ConvexusPoolReadOnly 
{
  // ================================================
  // Consts
  // ================================================
  public ConvexusPoolReadOnly () {}

  /**
   * @notice Swap token0 for token1, or token1 for token0
   * 
   * Access: Everyone
   * 
   * @dev The caller of this method receives a callback in the form of convexusSwapCallback
   * @param recipient The address to receive the output of the swap
   * @param zeroForOne The direction of the swap, true for token0 to token1, false for token1 to token0
   * @param amountSpecified The amount of the swap, which implicitly configures the swap as exact input (positive), or exact output (negative)
   * @param sqrtPriceLimitX96 The Q64.96 sqrt price limit. If zero for one, the price cannot be less than this value after the swap. If one for zero, the price cannot be greater than this value after the swap.
   * @param data Any data to be passed through to the callback
   * @return amount0 The delta of the balance of token0 of the pool, exact when negative, minimum when positive
   * @return amount1 The delta of the balance of token1 of the pool, exact when negative, minimum when positive
   */
  @External(readonly = true)
  public SwapResult swap (
    Address pool,
    Address recipient,
    boolean zeroForOne,
    BigInteger amountSpecified,
    BigInteger sqrtPriceLimitX96,
    byte[] data
  ) {
    final Address caller = Context.getCaller();

    Context.require(!amountSpecified.equals(ZERO),
      "swap: amountSpecified must be different from zero");
    
    Slot0 slot0Start = IConvexusPool.slot0(pool);

    Context.require (
      zeroForOne
        ? sqrtPriceLimitX96.compareTo(slot0Start.sqrtPriceX96) < 0 && sqrtPriceLimitX96.compareTo(TickMath.MIN_SQRT_RATIO) > 0
        : sqrtPriceLimitX96.compareTo(slot0Start.sqrtPriceX96) > 0 && sqrtPriceLimitX96.compareTo(TickMath.MAX_SQRT_RATIO) < 0,
      "swap: Wrong sqrtPriceLimitX96"
    );

    SwapCache cache = new SwapCache(
      IConvexusPool.liquidity(pool),
      TimeUtils.now(),
      zeroForOne ? (slot0Start.feeProtocol % 16) : (slot0Start.feeProtocol >> 4),
      ZERO,
      ZERO,
      false
    );

    boolean exactInput = amountSpecified.compareTo(ZERO) > 0;

    SwapState state = new SwapState(
      amountSpecified,
      ZERO,
      slot0Start.sqrtPriceX96,
      slot0Start.tick,
      zeroForOne ? IConvexusPool.feeGrowthGlobal0X128(pool) : IConvexusPool.feeGrowthGlobal1X128(pool),
      ZERO,
      cache.liquidityStart
    );
    
    // continue swapping as long as we haven't used the entire input/output and haven't reached the price limit
    while (
      !state.amountSpecifiedRemaining.equals(ZERO) 
     && !state.sqrtPriceX96.equals(sqrtPriceLimitX96)
    ) {

      StepComputations step = new StepComputations();
      step.sqrtPriceStartX96 = state.sqrtPriceX96;

      var next = new TickBitmapReadOnly(pool).nextInitializedTickWithinOneWord(
        state.tick,
        IConvexusPool.settings(pool).tickSpacing,
        zeroForOne
      );
      
      step.tickNext = next.tickNext;
      step.initialized = next.initialized;
      
      // ensure that we do not overshoot the min/max tick, as the tick bitmap is not aware of these bounds
      if (step.tickNext < TickMath.MIN_TICK) {
        step.tickNext = TickMath.MIN_TICK;
      } else if (step.tickNext > TickMath.MAX_TICK) {
        step.tickNext = TickMath.MAX_TICK;
      }

      // get the price for the next tick
      step.sqrtPriceNextX96 = TickMath.getSqrtRatioAtTick(step.tickNext);

      // compute values to swap to the target tick, price limit, or point where input/output amount is exhausted
      var swapStep = SwapMath.computeSwapStep(
        state.sqrtPriceX96,
        (zeroForOne ? step.sqrtPriceNextX96.compareTo(sqrtPriceLimitX96) < 0 : step.sqrtPriceNextX96.compareTo(sqrtPriceLimitX96) > 0)
          ? sqrtPriceLimitX96
          : step.sqrtPriceNextX96,
        state.liquidity,
        state.amountSpecifiedRemaining,
        IConvexusPool.settings(pool).fee
      );

      state.sqrtPriceX96 = swapStep.sqrtRatioNextX96;
      step.amountIn = swapStep.amountIn;
      step.amountOut = swapStep.amountOut;
      step.feeAmount = swapStep.feeAmount;

      if (exactInput) {
        state.amountSpecifiedRemaining = state.amountSpecifiedRemaining.subtract(step.amountIn.add(step.feeAmount));
        state.amountCalculated = state.amountCalculated.subtract(step.amountOut);
      } else {
        state.amountSpecifiedRemaining = state.amountSpecifiedRemaining.add(step.amountOut);
        state.amountCalculated = state.amountCalculated.add((step.amountIn.add(step.feeAmount)));
      }
      
      // if the protocol fee is on, calculate how much is owed, decrement feeAmount, and increment protocolFee
      if (cache.feeProtocol > 0) {
        BigInteger delta = step.feeAmount.divide(BigInteger.valueOf(cache.feeProtocol));
        step.feeAmount = step.feeAmount.subtract(delta);
        state.protocolFee = state.protocolFee.add(delta);
      }

      // update global fee tracker
      if (state.liquidity.compareTo(ZERO) > 0) {
        state.feeGrowthGlobalX128 = state.feeGrowthGlobalX128.add(FullMath.mulDiv(step.feeAmount, FixedPoint128.Q128, state.liquidity));
      }
      
      // shift tick if we reached the next price
      if (state.sqrtPriceX96.equals(step.sqrtPriceNextX96)) {
        // if the tick is initialized, run the tick transition
        if (step.initialized) {
          // check for the placeholder value, which we replace with the actual value the first time the swap
          // crosses an initialized tick
          if (!cache.computedLatestObservation) {
            var result = new ObservationsReadOnly(pool).observeSingle(
              cache.blockTimestamp,
              ZERO,
              slot0Start.tick,
              slot0Start.observationIndex,
              cache.liquidityStart,
              slot0Start.observationCardinality
            );
            cache.tickCumulative = result.tickCumulative;
            cache.secondsPerLiquidityCumulativeX128 = result.secondsPerLiquidityCumulativeX128;
            cache.computedLatestObservation = true;
          }
          BigInteger liquidityNet = new TicksReadOnly(pool).cross(
            step.tickNext,
            (zeroForOne ? state.feeGrowthGlobalX128 : IConvexusPool.feeGrowthGlobal0X128(pool)),
            (zeroForOne ? IConvexusPool.feeGrowthGlobal1X128(pool) : state.feeGrowthGlobalX128),
            cache.secondsPerLiquidityCumulativeX128,
            cache.tickCumulative,
            cache.blockTimestamp
          );
          // if we're moving leftward, we interpret liquidityNet as the opposite sign
          // safe because liquidityNet cannot be type(int128).min
          if (zeroForOne) liquidityNet = liquidityNet.negate();

          state.liquidity = LiquidityMath.addDelta(state.liquidity, liquidityNet);
        }

        state.tick = zeroForOne ? step.tickNext - 1 : step.tickNext;
      } else if (state.sqrtPriceX96 != step.sqrtPriceStartX96) {
        // recompute unless we're on a lower tick boundary (i.e. already transitioned ticks), and haven't moved
        state.tick = TickMath.getTickAtSqrtRatio(state.sqrtPriceX96);
      }
    }

    // update tick and write an oracle entry if the tick change
    Slot0 _slot0 = IConvexusPool.slot0(pool);
    if (state.tick != slot0Start.tick) {
      var result =
        new ObservationsReadOnly(pool).write(
          slot0Start.observationIndex,
          cache.blockTimestamp,
          slot0Start.tick,
          cache.liquidityStart,
          slot0Start.observationCardinality,
          slot0Start.observationCardinalityNext
        );
      _slot0.sqrtPriceX96 = state.sqrtPriceX96;
      _slot0.tick = state.tick;
      _slot0.observationIndex = result.indexUpdated;
      _slot0.observationCardinality = result.cardinalityUpdated;
    } else {
      // otherwise just update the price
      _slot0.sqrtPriceX96 = state.sqrtPriceX96;
    }

    BigInteger amount0;
    BigInteger amount1;

    if (zeroForOne == exactInput) {
      amount0 = amountSpecified.subtract(state.amountSpecifiedRemaining);
      amount1 = state.amountCalculated;
    } else {
      amount0 = state.amountCalculated;
      amount1 = amountSpecified.subtract(state.amountSpecifiedRemaining);
    }

    // do the transfers and collect payment
    if (zeroForOne) {
      return IConvexusPoolCalleeReadOnly.convexusSwapCallbackReadonly(caller, amount0, amount1, data, _slot0.sqrtPriceX96, _slot0.tick);
    } else {
      return IConvexusPoolCalleeReadOnly.convexusSwapCallbackReadonly(caller, amount0, amount1, data, _slot0.sqrtPriceX96, _slot0.tick);
    }
  }

  // ================================================
  // Public variable getters
  // ================================================
  @External(readonly = true)
  public String name() {
      return "Convexus Pool Readonly";
  }
}
