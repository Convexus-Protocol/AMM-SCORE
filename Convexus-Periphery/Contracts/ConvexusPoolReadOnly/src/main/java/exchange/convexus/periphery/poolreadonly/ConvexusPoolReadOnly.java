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
import exchange.convexus.core.factory.IConvexusFactory;
import exchange.convexus.core.interfaces.poolcallee.IConvexusPoolCalleeReadOnly;
import exchange.convexus.core.librairies.LiquidityMath;
import exchange.convexus.core.librairies.PositionLib;
import exchange.convexus.core.librairies.SqrtPriceMath;
import exchange.convexus.core.librairies.SwapMath;
import exchange.convexus.core.pool.contracts.models.Positions;
import exchange.convexus.librairies.FixedPoint128;
import exchange.convexus.librairies.FullMath;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.periphery.poolreadonly.cache.VarDBCache;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.ModifyPositionParams;
import exchange.convexus.pool.ModifyPositionResult;
import exchange.convexus.pool.PairAmounts;
import exchange.convexus.pool.Position;
import exchange.convexus.pool.PositionStorage;
import exchange.convexus.pool.Slot0;
import exchange.convexus.pool.StepComputations;
import exchange.convexus.pool.SwapCache;
import exchange.convexus.pool.SwapState;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.TimeUtils;
import score.Address;
import score.Context;
import score.annotation.External;
import exchange.convexus.positionmgr.INonFungiblePositionManager;
import exchange.convexus.positionmgr.PositionInformation;

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

  interface ConvexusPoolContract {
    PositionStorage _updatePosition (
      Address owner,
      int tickLower,
      int tickUpper,
      BigInteger liquidityDelta,
      int tick
    );

    ModifyPositionResult _modifyPosition (ModifyPositionParams params);

    PairAmounts burn (
      Address user,
      int tickLower,
      int tickUpper,
      BigInteger amount
    );
    
    public PairAmounts collect (
      Address caller,
      int tickLower,
      int tickUpper,
      BigInteger amount0Requested,
      BigInteger amount1Requested
    );
  }

  class FeeGrowthGlobal0X128Cache extends VarDBCache<BigInteger> {
    public FeeGrowthGlobal0X128Cache(Address target) {
      super(target);
    }

    @Override
    public BigInteger getExternal() {
      return IConvexusPool.feeGrowthGlobal0X128(target);
    }
  }
  
  class FeeGrowthGlobal1X128Cache extends VarDBCache<BigInteger> {
    public FeeGrowthGlobal1X128Cache(Address target) {
      super(target);
    }

    @Override
    public BigInteger getExternal() {
      return IConvexusPool.feeGrowthGlobal1X128(target);
    }
  }
  
  class Slot0Cache extends VarDBCache<Slot0> {
    public Slot0Cache(Address target) {
      super(target);
    }

    @Override
    public Slot0 getExternal() {
      return IConvexusPool.slot0(target);
    }
  }

  class LiquidityCache extends VarDBCache<BigInteger> {
    public LiquidityCache(Address target) {
      super(target);
    }

    @Override
    public BigInteger getExternal() {
      return IConvexusPool.liquidity(target);
    }
  }
  @External(readonly = true)
  public PairAmounts getOwedFeesNFT (
    Address user,
    Address nftManager,
    Address factory,
    BigInteger tokenId
  ) {
    PositionInformation tokenPos = INonFungiblePositionManager.positions(nftManager, tokenId);
    Address pool = IConvexusFactory.getPool(factory, tokenPos.token0, tokenPos.token1, tokenPos.fee);
    
    return getOwedFees (
      user, 
      pool, 
      tokenPos.tickLower, 
      tokenPos.tickUpper
    );
  }
  
  @External(readonly = true)
  public PairAmounts getOwedFees (
    Address user,
    Address pool,
    int tickLower,
    int tickUpper
  ) {
    PositionsCache this_positions = new PositionsCache(pool);
    FeeGrowthGlobal0X128Cache this_feeGrowthGlobal0X128 = new FeeGrowthGlobal0X128Cache(pool);
    FeeGrowthGlobal1X128Cache this_feeGrowthGlobal1X128 = new FeeGrowthGlobal1X128Cache(pool);
    Slot0Cache this_slot0 = new Slot0Cache(pool);
    ObservationsCache this_observations = new ObservationsCache(pool);
    LiquidityCache this_liquidity = new LiquidityCache(pool);
    BigInteger maxLiquidityPerTick = IConvexusPool.maxLiquidityPerTick(pool);
    int tickSpacing = IConvexusPool.tickSpacing(pool);
    TicksCache this_ticks = new TicksCache(pool);
    TickBitmapCache this_ticksBitmap = new TickBitmapCache(pool);
    
    byte[] positionKey = Positions.getKey(user, tickLower, tickUpper);
    Position.Info position = this_positions.get(positionKey);
    BigInteger liquidity = position.liquidity;

    ConvexusPoolContract contract = new ConvexusPoolContract() {
      @Override
      public PositionStorage _updatePosition (
        Address owner,
        int tickLower,
        int tickUpper,
        BigInteger liquidityDelta,
        int tick
      ) {
        byte[] positionKey = Positions.getKey(owner, tickLower, tickUpper);
        Position.Info position = this_positions.get(positionKey);
    
        BigInteger _feeGrowthGlobal0X128 = this_feeGrowthGlobal0X128.get();
        BigInteger _feeGrowthGlobal1X128 = this_feeGrowthGlobal1X128.get();
        Slot0 _slot0 = this_slot0.get();
    
        // if we need to update the ticks, do it
        boolean flippedLower = false;
        boolean flippedUpper = false;
        if (!liquidityDelta.equals(ZERO)) {
          BigInteger time = TimeUtils.now();
          var result = this_observations.observeSingle(
            time, 
            ZERO, 
            _slot0.tick, 
            _slot0.observationIndex, 
            this_liquidity.get(), 
            _slot0.observationCardinality
          );
    
          BigInteger tickCumulative = result.tickCumulative;
          BigInteger secondsPerLiquidityCumulativeX128 = result.secondsPerLiquidityCumulativeX128;
    
          TicksCache.UpdateResult resultLower  = this_ticks.update(
            tickLower,
            tick,
            liquidityDelta,
            _feeGrowthGlobal0X128,
            _feeGrowthGlobal1X128,
            secondsPerLiquidityCumulativeX128,
            tickCumulative,
            time,
            false,
            maxLiquidityPerTick
          );
          flippedLower = resultLower.flipped;
          
          TicksCache.UpdateResult resultUpper = this_ticks.update(
            tickUpper,
            tick,
            liquidityDelta,
            _feeGrowthGlobal0X128,
            _feeGrowthGlobal1X128,
            secondsPerLiquidityCumulativeX128,
            tickCumulative,
            time,
            true,
            maxLiquidityPerTick
          );
          flippedUpper = resultUpper.flipped;
          
          if (flippedLower) {
            this_ticksBitmap.flipTick(tickLower, tickSpacing);
          }
          if (flippedUpper) {
            this_ticksBitmap.flipTick(tickUpper, tickSpacing);
          }
        }
    
        var result = this_ticks.getFeeGrowthInside(tickLower, tickUpper, tick, _feeGrowthGlobal0X128, _feeGrowthGlobal1X128);
        BigInteger feeGrowthInside0X128 = result.feeGrowthInside0X128;
        BigInteger feeGrowthInside1X128 = result.feeGrowthInside1X128;
    
        PositionLib.update(position, liquidityDelta, feeGrowthInside0X128, feeGrowthInside1X128);
    
        // clear any tick data that is no longer needed
        if (liquidityDelta.compareTo(ZERO) < 0) {
          if (flippedLower) {
            this_ticks.clear(tickLower);
          }
          if (flippedUpper) {
            this_ticks.clear(tickUpper);
          }
        }
    
        this_positions.set(positionKey, position);
        return new PositionStorage(position, positionKey);
      }

      @Override
      public ModifyPositionResult _modifyPosition (
        ModifyPositionParams params
      ) {
        checkTicks(params.tickLower, params.tickUpper);

        Slot0 _slot0 = this_slot0.get();

        var positionStorage = _updatePosition(
          params.owner,
          params.tickLower,
          params.tickUpper,
          params.liquidityDelta,
          _slot0.tick
        );

        BigInteger amount0 = ZERO;
        BigInteger amount1 = ZERO;

        if (!params.liquidityDelta.equals(ZERO)) {
          if (_slot0.tick < params.tickLower) {
            // current tick is below the passed range; liquidity can only become in range by crossing from left to
            // right, when we'll need _more_ token0 (it's becoming more valuable) so user must provide it
            amount0 = SqrtPriceMath.getAmount0Delta(
              TickMath.getSqrtRatioAtTick(params.tickLower),
              TickMath.getSqrtRatioAtTick(params.tickUpper),
              params.liquidityDelta
            );
          } else if (_slot0.tick < params.tickUpper) {
            // current tick is inside the passed range
            BigInteger liquidityBefore = this_liquidity.get();

            // write an oracle entry
            var writeResult = this_observations.write(
              _slot0.observationIndex,
              TimeUtils.now(),
              _slot0.tick,
              liquidityBefore,
              _slot0.observationCardinality,
              _slot0.observationCardinalityNext
            );

            _slot0.observationIndex = writeResult.indexUpdated;
            _slot0.observationCardinality = writeResult.cardinalityUpdated;
            this_slot0.set(_slot0);

            amount0 = SqrtPriceMath.getAmount0Delta(
              _slot0.sqrtPriceX96,
              TickMath.getSqrtRatioAtTick(params.tickUpper),
              params.liquidityDelta
            );
            amount1 = SqrtPriceMath.getAmount1Delta(
              TickMath.getSqrtRatioAtTick(params.tickLower),
              _slot0.sqrtPriceX96,
              params.liquidityDelta
            );

            BigInteger newLiquidity = LiquidityMath.addDelta(liquidityBefore, params.liquidityDelta);
            this_liquidity.set(newLiquidity);
          } else {
            // current tick is above the passed range; liquidity can only become in range by crossing from right to
            // left, when we'll need _more_ token1 (it's becoming more valuable) so user must provide it
            amount1 = SqrtPriceMath.getAmount1Delta(
              TickMath.getSqrtRatioAtTick(params.tickLower),
              TickMath.getSqrtRatioAtTick(params.tickUpper),
              params.liquidityDelta
            );
          }
        }

        return new ModifyPositionResult(positionStorage, amount0, amount1);
      }

      @Override
      public PairAmounts burn (
        Address user,
        int tickLower,
        int tickUpper,
        BigInteger amount
      ) {
        var result = _modifyPosition(new ModifyPositionParams(
          user,
          tickLower,
          tickUpper,
          amount.negate()
        ));

        BigInteger amount0 = result.amount0.negate();
        BigInteger amount1 = result.amount1.negate();
        
        Position.Info position = result.positionStorage.position;
        byte[] positionKey = result.positionStorage.key;

        if (amount0.compareTo(ZERO) > 0 || amount1.compareTo(ZERO) > 0) {
          position.tokensOwed0 = position.tokensOwed0.add(amount0);
          position.tokensOwed1 = position.tokensOwed1.add(amount1);
          this_positions.set(positionKey, position);
        }

        return new PairAmounts(amount0, amount1);
      }

      @Override
      public PairAmounts collect (
        Address caller,
        int tickLower,
        int tickUpper,
        BigInteger amount0Requested,
        BigInteger amount1Requested
      ) {
        // we don't need to checkTicks here, because invalid positions will never have non-zero tokensOwed{0,1}
        byte[] key = Positions.getKey(caller, tickLower, tickUpper);
        Position.Info position = this_positions.get(key);

        BigInteger amount0 = amount0Requested.compareTo(position.tokensOwed0) > 0 ? position.tokensOwed0 : amount0Requested;
        BigInteger amount1 = amount1Requested.compareTo(position.tokensOwed1) > 0 ? position.tokensOwed1 : amount1Requested;

        if (amount0.compareTo(ZERO) > 0) {
          position.tokensOwed0 = position.tokensOwed0.subtract(amount0);
          this_positions.set(key, position);
        }
        if (amount1.compareTo(ZERO) > 0) {
          position.tokensOwed1 = position.tokensOwed1.subtract(amount1);
          this_positions.set(key, position);
        }

        return new PairAmounts(amount0, amount1);
      }
    };

    // Simulate nonFungiblePositionManager burn of the whole liquidity
    if (liquidity.compareTo(ZERO) > 0) {
      contract.burn(user, tickLower, tickUpper, liquidity);
    }

    return contract.collect(user, tickLower, tickUpper, IntUtils.MAX_UINT256, IntUtils.MAX_UINT256);
  }


  // ================================================
  // Checks
  // ================================================
  private void checkTicks (int tickLower, int tickUpper) {
    Context.require(tickLower < tickUpper, 
      "checkTicks: tickLower must be lower than tickUpper");
    Context.require(tickLower >= TickMath.MIN_TICK, 
      "checkTicks: tickLower lower than expected");
    Context.require(tickUpper <= TickMath.MAX_TICK, 
      "checkTicks: tickUpper greater than expected");
  }

  // ================================================
  // Public variable getters
  // ================================================
  @External(readonly = true)
  public String name() {
      return "Convexus Pool Readonly";
  }
}
