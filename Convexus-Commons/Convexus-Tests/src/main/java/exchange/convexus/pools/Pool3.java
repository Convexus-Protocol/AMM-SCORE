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

package exchange.convexus.pools;

import static exchange.convexus.librairies.BlockTimestamp._blockTimestamp;
import static exchange.convexus.utils.IntUtils.uint256;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import exchange.convexus.factory.Parameters;
import exchange.convexus.librairies.FixedPoint128;
import exchange.convexus.librairies.FullMath;
import exchange.convexus.librairies.LiquidityMath;
import exchange.convexus.librairies.Observations;
import exchange.convexus.librairies.ObserveResult;
import exchange.convexus.librairies.Oracle;
import exchange.convexus.librairies.PairAmounts;
import exchange.convexus.librairies.Position;
import exchange.convexus.librairies.Positions;
import exchange.convexus.librairies.SqrtPriceMath;
import exchange.convexus.librairies.Tick;
import exchange.convexus.librairies.TickBitmap;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.librairies.Ticks;
import exchange.convexus.pool.ModifyPositionParams;
import exchange.convexus.pool.ModifyPositionResult;
import exchange.convexus.pool.PositionStorage;
import exchange.convexus.pool.ProtocolFees;
import exchange.convexus.pool.Slot0;
import exchange.convexus.pool.SnapshotCumulativesInsideResult;
import exchange.convexus.pool.StepComputations;
import exchange.convexus.pool.SwapCache;
import exchange.convexus.pool.SwapMath;
import exchange.convexus.pool.SwapState;
import exchange.convexus.utils.ReentrancyLock;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class Pool3 extends ConvexusPool3 {
    public Pool3(Address token0, Address token1, Address factory, int fee, int tickSpacing) {
        super(new Parameters(factory, token0, token1, fee, tickSpacing));
    }
}

abstract class ConvexusPool3 {

    // ================================================
    // Consts
    // ================================================
    
    // Contract class name
    public static final String NAME = "ConvexusPool";

    // Contract name
    private final String name;

    // The contract that deployed the pool
    private final Address factory;

    // The first of the two tokens of the pool, sorted by address
    private final Address token0;

    // The second of the two tokens of the pool, sorted by address
    private final Address token1;

    // The pool's fee in hundredths of a bip, i.e. 1e-6
    private final int fee;

    // The pool tick spacing
    // @dev Ticks can only be used at multiples of this value, minimum of 1 and always positive
    // e.g.: a tickSpacing of 3 means ticks can be initialized every 3rd tick, i.e., ..., -6, -3, 0, 3, 6, ...
    // This value is an int to avoid casting even though it is always positive.
    private final int tickSpacing;

    // The maximum amount of position liquidity that can use any tick in the range
    // @dev This parameter is enforced per tick to prevent liquidity from overflowing an int at any point, and
    // also prevents out-of-range liquidity from being used to prevent adding in-range liquidity to a pool
    // @return The max amount of liquidity per tick
    private final BigInteger maxLiquidityPerTick;

    // ================================================
    // DB Variables
    // ================================================
    // The 0th storage slot in the pool stores many values, and is exposed as a single method to save steps when accessed externally.
    // This value may not always be equal to SqrtTickMath.getTickAtSqrtRatio(sqrtPriceX96) if the price is on a tick
    // boundary.
    // Encoded as two 4 bit values, where the protocol fee of token1 is shifted 4 bits and the protocol fee of token0
    // is the lower 4 bits. Used as the denominator of a fraction of the swap fee, e.g. 4 means 1/4th of the swap fee.
    // unlocked Whether the pool is currently locked to reentrancy
    private final VarDB<Slot0> slot0 = Context.newVarDB(NAME + "_slot0", Slot0.class);

    // whether the pool is locked
    private final ReentrancyLock poolLock = new ReentrancyLock(NAME + "_poolLock");
    
    // The fee growth as a Q128.128 fees of token0 collected per unit of liquidity for the entire life of the pool
    protected final VarDB<BigInteger> feeGrowthGlobal0X128 = Context.newVarDB(NAME + "_feeGrowthGlobal0X128", BigInteger.class);
    // The fee growth as a Q128.128 fees of token1 collected per unit of liquidity for the entire life of the pool
    protected final VarDB<BigInteger> feeGrowthGlobal1X128 = Context.newVarDB(NAME + "_feeGrowthGlobal1X128", BigInteger.class);
    
    // The amounts of token0 and token1 that are owed to the protocol
    private final VarDB<ProtocolFees> protocolFees = Context.newVarDB(NAME + "_protocolFees", ProtocolFees.class);
    
    // The amounts of token0 and token1 that are owed to the protocol
    private final VarDB<BigInteger> liquidity = Context.newVarDB(NAME + "_liquidity", BigInteger.class);

    // Look up information about a specific tick in the pool
    private final Ticks ticks = new Ticks();

    // Returns 256 packed tick initialized boolean values. See TickBitmap for more information
    private final TickBitmap tickBitmap = new TickBitmap();
    
    // Returns the information about a position by the position's key
    private final Positions positions = new Positions();

    // Returns data about a specific observation index
    private final Observations observations = new Observations();

    // ================================================
    // Event Logs
    // ================================================
    /**
     * @notice Emitted by the pool for increases to the number of observations that can be stored
     * @dev observationCardinalityNext is not the observation cardinality until an observation is written at the index
     * just before a mint/swap/burn.
     * @param observationCardinalityNextOld The previous value of the next observation cardinality
     * @param observationCardinalityNextNew The updated value of the next observation cardinality
     */
    @EventLog
    protected void IncreaseObservationCardinalityNext(
        int observationCardinalityNextOld,
        int observationCardinalityNextNew
    ) {}
  
    /**
     * @notice Emitted exactly once by a pool when #initialize is first called on the pool
     * @dev Mint/Burn/Swap cannot be emitted by the pool before Initialize
     * @param sqrtPriceX96 The initial sqrt price of the pool, as a Q64.96
     * @param tick The initial tick of the pool, i.e. log base 1.0001 of the starting price of the pool
     */
    @EventLog
    protected void Initialize(
        BigInteger sqrtPriceX96,
        int tick
    ) {}
    
    /**
     * @notice Emitted when liquidity is minted for a given position
     * @param sender The address that minted the liquidity
     * @param owner The owner of the position and recipient of any minted liquidity
     * @param tickLower The lower tick of the position
     * @param tickUpper The upper tick of the position
     * @param amount The amount of liquidity minted to the position range
     * @param amount0 How much token0 was required for the minted liquidity
     * @param amount1 How much token1 was required for the minted liquidity
     */
    @EventLog(indexed = 3)
    protected void Mint(
        Address recipient, 
        int tickLower, 
        int tickUpper, 
        Address sender, 
        BigInteger amount,
        BigInteger amount0, 
        BigInteger amount1
    ) {}

    /**
     * @notice Emitted when fees are collected by the owner of a position
     * @dev Collect events may be emitted with zero amount0 and amount1 when the caller chooses not to collect fees
     * @param owner The owner of the position for which fees are collected
     * @param tickLower The lower tick of the position
     * @param tickUpper The upper tick of the position
     * @param amount0 The amount of token0 fees collected
     * @param amount1 The amount of token1 fees collected
     */
    @EventLog(indexed = 3)
    protected void Collect(
        Address caller, 
        int tickLower, 
        int tickUpper, 
        Address recipient, 
        BigInteger amount0,
        BigInteger amount1
    ) {}
  
    /**
     * @notice Emitted when a position's liquidity is removed
     * @dev Does not withdraw any fees earned by the liquidity position, which must be withdrawn via #collect
     * @param owner The owner of the position for which liquidity is removed
     * @param tickLower The lower tick of the position
     * @param tickUpper The upper tick of the position
     * @param amount The amount of liquidity to remove
     * @param amount0 The amount of token0 withdrawn
     * @param amount1 The amount of token1 withdrawn
     */
    @EventLog(indexed = 3)
    protected void Burn(
        Address caller, 
        int tickLower, 
        int tickUpper, 
        BigInteger amount, 
        BigInteger amount0,
        BigInteger amount1
    ) {}

    /**
     * @notice Emitted by the pool for any swaps between token0 and token1
     * @param sender The address that initiated the swap call, and that received the callback
     * @param recipient The address that received the output of the swap
     * @param amount0 The delta of the token0 balance of the pool
     * @param amount1 The delta of the token1 balance of the pool
     * @param sqrtPriceX96 The sqrt(price) of the pool after the swap, as a Q64.96
     * @param liquidity The liquidity of the pool after the swap
     * @param tick The log base 1.0001 of price of the pool after the swap
     */
    @EventLog(indexed = 2)
    protected void Swap(
        Address sender,
        Address recipient,
        BigInteger amount0,
        BigInteger amount1,
        BigInteger sqrtPriceX96,
        BigInteger liquidity,
        int tick
    ) {}

    /**
     * @notice Emitted by the pool for any flashes of token0/token1
     * @param sender The address that initiated the swap call, and that received the callback
     * @param recipient The address that received the tokens from flash
     * @param amount0 The amount of token0 that was flashed
     * @param amount1 The amount of token1 that was flashed
     * @param paid0 The amount of token0 paid for the flash, which can exceed the amount0 plus the fee
     * @param paid1 The amount of token1 paid for the flash, which can exceed the amount1 plus the fee
     */
    @EventLog(indexed = 2)
    protected void Flash(
        Address sender,
        Address recipient,
        BigInteger amount0,
        BigInteger amount1,
        BigInteger paid0,
        BigInteger paid1
    ) {}

    /**
     * @notice Emitted when the protocol fee is changed by the pool
     * @param feeProtocol0Old The previous value of the token0 protocol fee
     * @param feeProtocol1Old The previous value of the token1 protocol fee
     * @param feeProtocol0New The updated value of the token0 protocol fee
     * @param feeProtocol1New The updated value of the token1 protocol fee
     */    
    @EventLog
    protected void SetFeeProtocol(
        int feeProtocol0Old, 
        int feeProtocol1Old, 
        int feeProtocol0New, 
        int feeProtocol1New
    ) {}

    /**
     * @notice Emitted when the collected protocol fees are withdrawn by the factory owner
     * @param sender The address that collects the protocol fees
     * @param recipient The address that receives the collected protocol fees
     * @param amount0 The amount of token0 protocol fees that is withdrawn
     * @param amount0 The amount of token1 protocol fees that is withdrawn
     */
    @EventLog(indexed = 2)
    protected void CollectProtocol(
        Address sender, 
        Address recipient, 
        BigInteger amount0, 
        BigInteger amount1
    ) {}


    // ================================================
    // Methods
    // ================================================
    /**
     * @notice Contract constructor
     * @dev This contract should be not deployed, as this class is abstract anyway. 
     * See {@code ConvexusPoolFactored} constructor for the actual pool deployed on the network
     */
    protected ConvexusPool3 (Parameters parameters) {
        this.factory = parameters.factory;
        this.token0 = parameters.token0;
        this.token1 = parameters.token1;
        this.fee = parameters.fee;
        this.tickSpacing = parameters.tickSpacing;

        this.maxLiquidityPerTick = Tick.tickSpacingToMaxLiquidityPerTick(this.tickSpacing);
        this.name = "Convexus Pool " + Context.call(this.token0, "symbol") + "/" + Context.call(this.token1, "symbol");

        // Default values
        if (this.liquidity.get() == null) {
            this.liquidity.set(ZERO);
        }
        if (this.feeGrowthGlobal0X128.get() == null) {
            this.feeGrowthGlobal0X128.set(ZERO);
        }
        if (this.feeGrowthGlobal1X128.get() == null) {
            this.feeGrowthGlobal1X128.set(ZERO);
        }
        if (this.protocolFees.get() == null) {
            this.protocolFees.set(new ProtocolFees(ZERO, ZERO));
        }

        // locked by default
        if (this.poolLock.get() == null) {
            this.poolLock.lock(true);
        }
    }

    private void checkTicks (int tickLower, int tickUpper) {
        Context.require(tickLower < tickUpper, 
            "checkTicks: tickLower must be lower than tickUpper");
        Context.require(tickLower >= TickMath.MIN_TICK, 
            "checkTicks: tickLower lower than expected");
        Context.require(tickUpper <= TickMath.MAX_TICK, 
            "checkTicks: tickUpper greater than expected");
    }

    /**
     * @notice Get the pool's balance of token0
     */
    private BigInteger balance0 () {
        return (BigInteger) Context.call(this.token0, "balanceOf", Context.getAddress());
    }

    /**
     * @notice Get the pool's balance of token1
     */
    private BigInteger balance1 () {
        return (BigInteger) Context.call(this.token1, "balanceOf", Context.getAddress());
    }
    
    /**
     * @notice Returns a snapshot of the tick cumulative, seconds per liquidity and seconds inside a tick range
     * 
     * Access: Everyone
     * 
     * @dev Snapshots must only be compared to other snapshots, taken over a period for which a position existed.
     * I.e., snapshots cannot be compared if a position is not held for the entire period between when the first
     * snapshot is taken and the second snapshot is taken.
     * @param tickLower The lower tick of the range
     * @param tickUpper The upper tick of the range
     * @return tickCumulativeInside The snapshot of the tick accumulator for the range
     * @return secondsPerLiquidityInsideX128 The snapshot of seconds per liquidity for the range
     * @return secondsInside The snapshot of seconds per liquidity for the range
     */
    @External(readonly = true)
    public SnapshotCumulativesInsideResult snapshotCumulativesInside (int tickLower, int tickUpper) {
        checkTicks(tickLower, tickUpper);

        Tick.Info lower = ticks.get(tickLower);
        Tick.Info upper = ticks.get(tickUpper);

        BigInteger tickCumulativeLower = lower.tickCumulativeOutside;
        BigInteger tickCumulativeUpper = upper.tickCumulativeOutside;
        BigInteger secondsPerLiquidityOutsideLowerX128 = lower.secondsPerLiquidityOutsideX128;
        BigInteger secondsPerLiquidityOutsideUpperX128 = upper.secondsPerLiquidityOutsideX128;
        BigInteger secondsOutsideLower = lower.secondsOutside;
        BigInteger secondsOutsideUpper = upper.secondsOutside;

        Context.require(lower.initialized, 
            "snapshotCumulativesInside: lower not initialized");
        Context.require(upper.initialized, 
            "snapshotCumulativesInside: upper not initialized");

        Slot0 _slot0 = this.slot0.get();

        if (_slot0.tick < tickLower) {
            return new SnapshotCumulativesInsideResult(
                tickCumulativeLower.subtract(tickCumulativeUpper),
                secondsPerLiquidityOutsideLowerX128.subtract(secondsPerLiquidityOutsideUpperX128),
                secondsOutsideLower.subtract(secondsOutsideUpper)
            );
        } else if (_slot0.tick < tickUpper) {
            BigInteger time = _blockTimestamp();
            Observations.ObserveSingleResult result = observations.observeSingle(
                time, 
                ZERO, 
                _slot0.tick, 
                _slot0.observationIndex, 
                this.liquidity.get(), 
                _slot0.observationCardinality
            );
            BigInteger tickCumulative = result.tickCumulative;
            BigInteger secondsPerLiquidityCumulativeX128 = result.secondsPerLiquidityCumulativeX128;

            return new SnapshotCumulativesInsideResult(
                tickCumulative.subtract(tickCumulativeLower).subtract(tickCumulativeUpper),
                secondsPerLiquidityCumulativeX128.subtract(secondsPerLiquidityOutsideLowerX128).subtract(secondsPerLiquidityOutsideUpperX128),
                time.subtract(secondsOutsideLower).subtract(secondsOutsideUpper)
            );
        } else {
            return new SnapshotCumulativesInsideResult (
                tickCumulativeUpper.subtract(tickCumulativeLower),
                secondsPerLiquidityOutsideUpperX128.subtract(secondsPerLiquidityOutsideLowerX128),
                secondsOutsideUpper.subtract(secondsOutsideLower)
            );
        }
    }

    /**
     * @notice Returns the cumulative tick and liquidity as of each timestamp `secondsAgo` from the current block timestamp
     * 
     * Access: Everyone
     * 
     * @dev To get a time weighted average tick or liquidity-in-range, you must call this with two values, one representing
     * the beginning of the period and another for the end of the period. E.g., to get the last hour time-weighted average tick,
     * you must call it with secondsAgos = [3600, 0].
     * @dev The time weighted average tick represents the geometric time weighted average price of the pool, in
     * log base sqrt(1.0001) of token1 / token0. The TickMath library can be used to go from a tick value to a ratio.
     * @param secondsAgos From how long ago each cumulative tick and liquidity value should be returned
     * @return tickCumulatives Cumulative tick values as of each `secondsAgos` from the current block timestamp
     * @return secondsPerLiquidityCumulativeX128s Cumulative seconds per liquidity-in-range value as of each `secondsAgos` from the current block
     * timestamp
     */
    @External(readonly = true)
    public ObserveResult observe (BigInteger[] secondsAgos) {
        Slot0 _slot0 = this.slot0.get();
        return observations.observe(
            _blockTimestamp(), 
            secondsAgos, 
            _slot0.tick, 
            _slot0.observationIndex, 
            this.liquidity.get(), 
            _slot0.observationCardinality
        );
    }

    /**
     * @notice Increase the maximum number of price and liquidity observations that this pool will store
     * 
     * Access: Everyone
     * 
     * @dev This method is no-op if the pool already has an observationCardinalityNext greater than or equal to
     * the input observationCardinalityNext.
     * @param observationCardinalityNext The desired minimum number of observations for the pool to store
     */
    @External
    public void increaseObservationCardinalityNext (int observationCardinalityNext) {
        poolLock.lock(true);

        Slot0 _slot0 = this.slot0.get();
        int observationCardinalityNextOld = _slot0.observationCardinalityNext;
        int observationCardinalityNextNew = observations.grow(observationCardinalityNextOld, observationCardinalityNext);

        _slot0.observationCardinalityNext = observationCardinalityNextNew;
        this.slot0.set(_slot0);

        if (observationCardinalityNextOld != observationCardinalityNextNew) {
            this.IncreaseObservationCardinalityNext(observationCardinalityNextOld, observationCardinalityNextNew);
        }

        poolLock.lock(false);
    }

    /**
     * @notice Sets the initial price for the pool
     * 
     * Access: Everyone
     * 
     * @dev Price is represented as a sqrt(amountToken1/amountToken0) Q64.96 value
     * @param sqrtPriceX96 the initial sqrt price of the pool as a Q64.96
     * @dev not locked because it initializes unlocked
     */
    @External
    public void initialize (BigInteger sqrtPriceX96) {
        Context.require(this.slot0.get() == null, 
            "initialize: this pool is already initialized");

        int tick = TickMath.getTickAtSqrtRatio(sqrtPriceX96);
        
        var result = observations.initialize(_blockTimestamp());

        this.slot0.set(new Slot0(
            sqrtPriceX96,
            tick,
            0,
            result.cardinality,
            result.cardinalityNext,
            0
        ));

        // Unlock the pool
        this.poolLock.lock(false);

        this.Initialize(sqrtPriceX96, tick);
    }

    /**
     * @dev Gets and updates a position with the given liquidity delta
     * @param owner the owner of the position
     * @param tickLower the lower tick of the position's tick range
     * @param tickUpper the upper tick of the position's tick range
     * @param tick the current tick, passed to avoid sloads
     */
    private PositionStorage _updatePosition (
        Address owner,
        int tickLower,
        int tickUpper,
        BigInteger liquidityDelta,
        int tick
    ) {
        byte[] positionKey = Positions.getKey(owner, tickLower, tickUpper);
        Position.Info position = this.positions.get(positionKey);

        BigInteger _feeGrowthGlobal0X128 = this.feeGrowthGlobal0X128.get();
        BigInteger _feeGrowthGlobal1X128 = this.feeGrowthGlobal1X128.get();
        Slot0 _slot0 = this.slot0.get();

        // if we need to update the ticks, do it
        boolean flippedLower = false;
        boolean flippedUpper = false;
        if (!liquidityDelta.equals(ZERO)) {
            BigInteger time = _blockTimestamp();
            var result = this.observations.observeSingle(
                time, 
                ZERO, 
                _slot0.tick, 
                _slot0.observationIndex, 
                this.liquidity.get(), 
                _slot0.observationCardinality
            );

            BigInteger tickCumulative = result.tickCumulative;
            BigInteger secondsPerLiquidityCumulativeX128 = result.secondsPerLiquidityCumulativeX128;

            flippedLower = ticks.update(
                tickLower,
                tick,
                liquidityDelta,
                _feeGrowthGlobal0X128,
                _feeGrowthGlobal1X128,
                secondsPerLiquidityCumulativeX128,
                tickCumulative,
                time,
                false,
                this.maxLiquidityPerTick
            );
            
            flippedUpper = ticks.update(
                tickUpper,
                tick,
                liquidityDelta,
                _feeGrowthGlobal0X128,
                _feeGrowthGlobal1X128,
                secondsPerLiquidityCumulativeX128,
                tickCumulative,
                time,
                true,
                this.maxLiquidityPerTick
            );
            
            if (flippedLower) {
                tickBitmap.flipTick(tickLower, tickSpacing);
            }
            if (flippedUpper) {
                tickBitmap.flipTick(tickUpper, tickSpacing);
            }
        }

        var result = this.ticks.getFeeGrowthInside(tickLower, tickUpper, tick, _feeGrowthGlobal0X128, _feeGrowthGlobal1X128);
        BigInteger feeGrowthInside0X128 = result.feeGrowthInside0X128;
        BigInteger feeGrowthInside1X128 = result.feeGrowthInside1X128;

        position.update(liquidityDelta, feeGrowthInside0X128, feeGrowthInside1X128);

        // clear any tick data that is no longer needed
        if (liquidityDelta.compareTo(ZERO) < 0) {
            if (flippedLower) {
                this.ticks.clear(tickLower);
            }
            if (flippedUpper) {
                this.ticks.clear(tickUpper);
            }
        }

        this.positions.set(positionKey, position);
        return new PositionStorage(position, positionKey);
    }

    /**
     * @dev Effect some changes to a position
     * @param params the position details and the change to the position's liquidity to effect
     * @return position a storage pointer referencing the position with the given owner and tick range
     * @return amount0 the amount of token0 owed to the pool, negative if the pool should pay the recipient
     * @return amount1 the amount of token1 owed to the pool, negative if the pool should pay the recipient
     */
    private ModifyPositionResult _modifyPosition (ModifyPositionParams params) {
        checkTicks(params.tickLower, params.tickUpper);

        Slot0 _slot0 = this.slot0.get();

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
                BigInteger liquidityBefore = this.liquidity.get();

                // write an oracle entry
                var writeResult = observations.write(
                    _slot0.observationIndex,
                    _blockTimestamp(),
                    _slot0.tick,
                    liquidityBefore,
                    _slot0.observationCardinality,
                    _slot0.observationCardinalityNext
                );

                _slot0.observationIndex = writeResult.indexUpdated;
                _slot0.observationCardinality = writeResult.cardinalityUpdated;
                this.slot0.set(_slot0);

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

                this.liquidity.set(LiquidityMath.addDelta(liquidityBefore, params.liquidityDelta));
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

    /**
     * @notice Adds liquidity for the given recipient/tickLower/tickUpper position
     * 
     * Access: Everyone
     * 
     * @dev The caller of this method receives a callback in the form of convexusMintCallback
     * in which they must pay any token0 or token1 owed for the liquidity. The amount of token0/token1 due depends
     * on tickLower, tickUpper, the amount of liquidity, and the current price.
     * @param recipient The address for which the liquidity will be created
     * @param tickLower The lower tick of the position in which to add liquidity
     * @param tickUpper The upper tick of the position in which to add liquidity
     * @param amount The amount of liquidity to mint
     * @param data Any data that should be passed through to the callback
     * @return amount0 The amount of token0 that was paid to mint the given amount of liquidity. Matches the value in the callback
     * @return amount1 The amount of token1 that was paid to mint the given amount of liquidity. Matches the value in the callback
     */
    @External
    public PairAmounts mint (
        Address recipient,
        int tickLower,
        int tickUpper,
        BigInteger amount,
        byte[] data
    ) {
        this.poolLock.lock(true);

        final Address caller = Context.getCaller();

        Context.require(amount.compareTo(ZERO) > 0,
            "mint: amount must be superior to 0");

        BigInteger amount0;
        BigInteger amount1;

        var result = _modifyPosition(new ModifyPositionParams(
            recipient,
            tickLower,
            tickUpper,
            amount
        ));

        amount0 = result.amount0;
        amount1 = result.amount1;

        BigInteger balance0Before = ZERO;
        BigInteger balance1Before = ZERO;
        
        if (amount0.compareTo(ZERO) > 0) {
            balance0Before = balance0();
        }
        if (amount1.compareTo(ZERO) > 0) {
            balance1Before = balance1();
        }

        Context.call(caller, "convexusMintCallback", amount0, amount1, data);

        if (amount0.compareTo(ZERO) > 0) {
            BigInteger expected = balance0Before.add(amount0);
            Context.require(expected.compareTo(balance0()) <= 0, "mint: M0");
        }
        if (amount1.compareTo(ZERO) > 0) {
            BigInteger expected = balance1Before.add(amount1);
            Context.require(expected.compareTo(balance1()) <= 0, "mint: M1");
        }

        this.Mint(recipient, tickLower, tickUpper, caller, amount, amount0, amount1);

        this.poolLock.lock(false);
        return new PairAmounts(amount0, amount1);
    }

    /**
     * @notice Collects tokens owed to a position
     * 
     * Access: Everyone
     * 
     * @dev Does not recompute fees earned, which must be done either via mint or burn of any amount of liquidity.
     * Collect must be called by the position owner. To withdraw only token0 or only token1, amount0Requested or
     * amount1Requested may be set to zero. To withdraw all tokens owed, caller may pass any value greater than the
     * actual tokens owed, e.g. type(uint128).max. Tokens owed may be from accumulated swap fees or burned liquidity.
     * @param recipient The address which should receive the fees collected
     * @param tickLower The lower tick of the position for which to collect fees
     * @param tickUpper The upper tick of the position for which to collect fees
     * @param amount0Requested How much token0 should be withdrawn from the fees owed
     * @param amount1Requested How much token1 should be withdrawn from the fees owed
     * @return amount0 The amount of fees collected in token0
     * @return amount1 The amount of fees collected in token1
     */
    @External
    public PairAmounts collect (
        Address recipient,
        int tickLower,
        int tickUpper,
        BigInteger amount0Requested,
        BigInteger amount1Requested
    ) {
        this.poolLock.lock(true);

        final Address caller = Context.getCaller();
        BigInteger amount0;
        BigInteger amount1;

        // we don't need to checkTicks here, because invalid positions will never have non-zero tokensOwed{0,1}
        byte[] key = Positions.getKey(caller, tickLower, tickUpper);
        Position.Info position = this.positions.get(key);

        amount0 = amount0Requested.compareTo(position.tokensOwed0) > 0 ? position.tokensOwed0 : amount0Requested;
        amount1 = amount1Requested.compareTo(position.tokensOwed1) > 0 ? position.tokensOwed1 : amount1Requested;

        if (amount0.compareTo(ZERO) > 0) {
            position.tokensOwed0 = position.tokensOwed0.subtract(amount0);
            this.positions.set(key, position);
            pay(this.token0, recipient, amount0);
        }
        if (amount1.compareTo(ZERO) > 0) {
            position.tokensOwed1 = position.tokensOwed1.subtract(amount1);
            this.positions.set(key, position);
            pay(this.token1, recipient, amount1);
        }

        this.Collect(caller, tickLower, tickUpper, recipient, amount0, amount1);

        this.poolLock.lock(false);
        return new PairAmounts(amount0, amount1);
    }

    private void pay (Address token, Address recipient, BigInteger amount) {
        Context.println("[Pool][pay][" + Context.call(token, "symbol") + "] " + amount);
        Context.call(token, "transfer", recipient, amount, "{\"method\": \"pay\"}".getBytes());
    }

    /**
     * @notice Burn liquidity from the sender and account tokens owed for the liquidity to the position
     * 
     * Access: Everyone
     * 
     * @dev Can be used to trigger a recalculation of fees owed to a position by calling with an amount of 0
     * @dev Fees must be collected separately via a call to #collect
     * @param tickLower The lower tick of the position for which to burn liquidity
     * @param tickUpper The upper tick of the position for which to burn liquidity
     * @param amount How much liquidity to burn
     * @return amount0 The amount of token0 sent to the recipient
     * @return amount1 The amount of token1 sent to the recipient
     */
    @External
    public PairAmounts burn (
        int tickLower,
        int tickUpper,
        BigInteger amount
    ) {
        this.poolLock.lock(true);
        final Address caller = Context.getCaller();

        var result = _modifyPosition(new ModifyPositionParams(
            caller,
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
            this.positions.set(positionKey, position);
        }

        this.Burn(caller, tickLower, tickUpper, amount, amount0, amount1);

        this.poolLock.lock(false);
        return new PairAmounts(amount0, amount1);
    }

    /**
     * @notice Swap token0 for token1, or token1 for token0
     * 
     * Access: Everyone
     * 
     * @dev The caller of this method receives a callback in the form of convexusSwapCallback
     * @param recipient The address to receive the output of the swap
     * @param zeroForOne The direction of the swap, true for token0 to token1, false for token1 to token0
     * @param amountSpecified The amount of the swap, which implicitly configures the swap as exact input (positive), or exact output (negative)
     * @param sqrtPriceLimitX96 The Q64.96 sqrt price limit. If zero for one, the price cannot be less than this
     * value after the swap. If one for zero, the price cannot be greater than this value after the swap
     * @param data Any data to be passed through to the callback
     * @return amount0 The delta of the balance of token0 of the pool, exact when negative, minimum when positive
     * @return amount1 The delta of the balance of token1 of the pool, exact when negative, minimum when positive
     */
    @External
    public PairAmounts swap (
        Address recipient,
        boolean zeroForOne,
        BigInteger amountSpecified,
        BigInteger sqrtPriceLimitX96,
        byte[] data
    ) {
        this.poolLock.lock(true);
        final Address caller = Context.getCaller();

        Context.require(!amountSpecified.equals(ZERO),
            "swap: amountSpecified must be different from zero");
        
        Slot0 slot0Start = this.slot0.get();

        Context.require(
            zeroForOne
                ? sqrtPriceLimitX96.compareTo(slot0Start.sqrtPriceX96) < 0 && sqrtPriceLimitX96.compareTo(TickMath.MIN_SQRT_RATIO) > 0
                : sqrtPriceLimitX96.compareTo(slot0Start.sqrtPriceX96) > 0 && sqrtPriceLimitX96.compareTo(TickMath.MAX_SQRT_RATIO) < 0,
            "swap: Wrong sqrtPriceLimitX96"
        );

        SwapCache cache = new SwapCache(
            this.liquidity.get(),
            _blockTimestamp(),
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
            zeroForOne ? feeGrowthGlobal0X128.get() : feeGrowthGlobal1X128.get(),
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

            var next = tickBitmap.nextInitializedTickWithinOneWord(
                state.tick,
                tickSpacing,
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
                fee
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
                        var result = observations.observeSingle(
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
                    BigInteger liquidityNet = ticks.cross(
                        step.tickNext,
                        (zeroForOne ? state.feeGrowthGlobalX128 : feeGrowthGlobal0X128.get()),
                        (zeroForOne ? feeGrowthGlobal1X128.get() : state.feeGrowthGlobalX128),
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
        if (state.tick != slot0Start.tick) {
            var result =
                observations.write(
                    slot0Start.observationIndex,
                    cache.blockTimestamp,
                    slot0Start.tick,
                    cache.liquidityStart,
                    slot0Start.observationCardinality,
                    slot0Start.observationCardinalityNext
                );
            Slot0 _slot0 = this.slot0.get();
            _slot0.sqrtPriceX96 = state.sqrtPriceX96;
            _slot0.tick = state.tick;
            _slot0.observationIndex = result.indexUpdated;
            _slot0.observationCardinality = result.cardinalityUpdated;
            this.slot0.set(_slot0);
        } else {
            // otherwise just update the price
            Slot0 _slot0 = this.slot0.get();
            _slot0.sqrtPriceX96 = state.sqrtPriceX96;
            this.slot0.set(_slot0);
        }

        // update liquidity if it changed
        if (cache.liquidityStart != state.liquidity) {
            liquidity.set(state.liquidity);
        }

        // update fee growth global and, if necessary, protocol fees
        // overflow is acceptable, protocol has to withdraw before it hits type(uint128).max fees
        if (zeroForOne) {
            this.feeGrowthGlobal0X128.set(state.feeGrowthGlobalX128);
            if (state.protocolFee.compareTo(ZERO) > 0) {
                var _protocolFees = this.protocolFees.get();
                _protocolFees.token0 = _protocolFees.token0.add(state.protocolFee);
                this.protocolFees.set(_protocolFees);
            }
        } else {
            this.feeGrowthGlobal1X128.set(state.feeGrowthGlobalX128);
            if (state.protocolFee.compareTo(ZERO) > 0) {
                var _protocolFees = this.protocolFees.get();
                _protocolFees.token1 = _protocolFees.token1.add(state.protocolFee);
                this.protocolFees.set(_protocolFees);
            }
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
            if (amount1.compareTo(ZERO) < 0) {
                pay(token1, recipient, amount1.negate());
            }

            BigInteger balance0Before = balance0();
            Context.call(caller, "convexusSwapCallback", amount0, amount1, data);

            Context.require(balance0Before.add(amount0).compareTo(balance0()) <= 0, 
                "swap: the callback didn't charge the payment (1)");
        } else {
            if (amount0.compareTo(ZERO) < 0) {
                pay(token0, recipient, amount0.negate());
            }

            BigInteger balance1Before = balance1();
            Context.call(caller, "convexusSwapCallback", amount0, amount1, data);

            Context.require(balance1Before.add(amount1).compareTo(balance1()) <= 0, 
                "swap: the callback didn't charge the payment (2)");
        }

        this.Swap(caller, recipient, amount0, amount1, state.sqrtPriceX96, state.liquidity, state.tick);
        this.poolLock.lock(false);

        return new PairAmounts(amount0, amount1);
    }

    /**
     * @notice Receive token0 and/or token1 and pay it back, plus a fee, in the callback
     * 
     * Access: Everyone
     * 
     * @dev The caller of this method receives a callback in the form of convexusFlashCallback
     * @dev Can be used to donate underlying tokens pro-rata to currently in-range liquidity providers by calling
     * with 0 amount{0,1} and sending the donation amount(s) from the callback
     * @param recipient The address which will receive the token0 and token1 amounts
     * @param amount0 The amount of token0 to send
     * @param amount1 The amount of token1 to send
     * @param data Any data to be passed through to the callback
     */
    @External
    public void flash (
        Address recipient,
        BigInteger amount0,
        BigInteger amount1,
        byte[] data
    ) {
        this.poolLock.lock(true);
        final Address caller = Context.getCaller();

        BigInteger _liquidity = this.liquidity.get();
        Context.require(_liquidity.compareTo(ZERO) > 0,
            "flash: no liquidity");
        
        final BigInteger TEN_E6 = BigInteger.valueOf(1000000);
        
        BigInteger fee0 = FullMath.mulDivRoundingUp(amount0, BigInteger.valueOf(fee), TEN_E6);
        BigInteger fee1 = FullMath.mulDivRoundingUp(amount1, BigInteger.valueOf(fee), TEN_E6);
        BigInteger balance0Before = balance0();
        BigInteger balance1Before = balance1();

        if (amount0.compareTo(ZERO) > 0) {
            pay(token0, recipient, amount0);
        }
        if (amount1.compareTo(ZERO) > 0) {
            pay(token1, recipient, amount1);
        }

        Context.call(caller, "convexusFlashCallback", fee0, fee1, data);

        BigInteger balance0After = balance0();
        BigInteger balance1After = balance1();

        Context.require(balance0Before.add(fee0).compareTo(balance0After) <= 0, 
            "flash: not enough token0 returned");
        
        Context.require(balance1Before.add(fee1).compareTo(balance1After) <= 0, 
            "flash: not enough token1 returned");

        // sub is safe because we know balanceAfter is gt balanceBefore by at least fee
        BigInteger paid0 = balance0After.subtract(balance0Before);
        BigInteger paid1 = balance1After.subtract(balance1Before);

        Slot0 _slot0 = this.slot0.get();

        if (paid0.compareTo(ZERO) > 0) {
            int feeProtocol0 = _slot0.feeProtocol % 16;
            BigInteger fees0 = feeProtocol0 == 0 ? ZERO : paid0.divide(BigInteger.valueOf(feeProtocol0));
            if (fees0.compareTo(ZERO) > 0) {
                var _protocolFees = protocolFees.get();
                _protocolFees.token0 = _protocolFees.token0.add(fees0);
                protocolFees.set(_protocolFees);
            }
            feeGrowthGlobal0X128.set(uint256(feeGrowthGlobal0X128.get().add(FullMath.mulDiv(paid0.subtract(fees0), FixedPoint128.Q128, _liquidity))));
        }
        if (paid1.compareTo(ZERO) > 0) {
            int feeProtocol1 = _slot0.feeProtocol >> 4;
            BigInteger fees1 = feeProtocol1 == 0 ? ZERO : paid1.divide(BigInteger.valueOf(feeProtocol1));
            if (fees1.compareTo(ZERO) > 0) {
                var _protocolFees = protocolFees.get();
                _protocolFees.token1 = _protocolFees.token1.add(fees1);
                protocolFees.set(_protocolFees);
            }
            feeGrowthGlobal1X128.set(uint256(feeGrowthGlobal1X128.get().add(FullMath.mulDiv(paid1.subtract(fees1), FixedPoint128.Q128, _liquidity))));
        }

        this.Flash(caller, recipient, amount0, amount1, paid0, paid1);
    
        this.poolLock.lock(false);
    }

    /**
     * @notice Set the denominator of the protocol's % share of the fees
     * 
     * Access control: Factory Owner
     * 
     * @param feeProtocol0 new protocol fee for token0 of the pool
     * @param feeProtocol1 new protocol fee for token1 of the pool
     */
    @External
    public void setFeeProtocol (
        int feeProtocol0,
        int feeProtocol1
    ) {
        this.poolLock.lock(true);

        // Access control
        this.onlyFactoryOwner();
        
        // Check user input for protocol fees
        Context.require(
            (feeProtocol0 == 0 || (feeProtocol0 >= 4 && feeProtocol0 <= 10)) &&
            (feeProtocol1 == 0 || (feeProtocol1 >= 4 && feeProtocol1 <= 10)),
            "setFeeProtocol: Bad fees amount"
        );

        // OK
        Slot0 _slot0 = this.slot0.get();
        int feeProtocolOld = _slot0.feeProtocol;
        _slot0.feeProtocol = feeProtocol0 + (feeProtocol1 << 4);
        this.slot0.set(_slot0);

        this.SetFeeProtocol(feeProtocolOld % 16, feeProtocolOld >> 4, feeProtocol0, feeProtocol1);

        this.poolLock.lock(false);
    }

    /**
     * @notice Collect the protocol fee accrued to the pool
     * 
     * Access control: Factory Owner
     * 
     * @param recipient The address to which collected protocol fees should be sent
     * @param amount0Requested The maximum amount of token0 to send, can be 0 to collect fees in only token1
     * @param amount1Requested The maximum amount of token1 to send, can be 0 to collect fees in only token0
     * @return amount0 The protocol fee collected in token0
     * @return amount1 The protocol fee collected in token1
     */
    @External
    public PairAmounts collectProtocol(
        Address recipient,
        BigInteger amount0Requested,
        BigInteger amount1Requested
    ) {
        this.poolLock.lock(true);

        // Access control
        this.onlyFactoryOwner();

        // OK
        var _protocolFees = protocolFees.get();
        final Address caller = Context.getCaller();

        BigInteger amount0 = amount0Requested.compareTo(_protocolFees.token0) > 0 ? _protocolFees.token0 : amount0Requested;
        BigInteger amount1 = amount1Requested.compareTo(_protocolFees.token1) > 0 ? _protocolFees.token1 : amount1Requested;
        
        if (amount0.compareTo(ZERO) > 0) {
            if (amount0.equals(_protocolFees.token0)) {
                // ensure that the slot is not cleared, for steps savings
                amount0 = amount0.subtract(BigInteger.ONE); 
            }
            _protocolFees.token0 = _protocolFees.token0.subtract(amount0);
            this.protocolFees.set(_protocolFees);
            pay(token0, recipient, amount0);
        }
        if (amount1.compareTo(ZERO) > 0) {
            if (amount1.equals(_protocolFees.token1)) {
                // ensure that the slot is not cleared, for steps savings
                amount1 = amount1.subtract(BigInteger.ONE); 
            }
            _protocolFees.token1 = _protocolFees.token1.subtract(amount1);
            this.protocolFees.set(_protocolFees);
            pay(token1, recipient, amount1);
        }

        this.CollectProtocol(caller, recipient, amount0, amount1);
        
        this.poolLock.lock(false);
        return new PairAmounts(amount0, amount1);
    }

    @External
    public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) throws Exception {
        Context.require(_from.isContract(), "tokenFallback: Pool shouldn't need to receive funds from EOA");
    }

    // ================================================
    // Checks
    // ================================================
    private void onlyFactoryOwner() {
        Context.require(Context.getCaller().equals(Context.call(this.factory, "owner")),
            "onlyFactoryOwner: Only owner can call this method");
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }

    @External(readonly = true)
    public Address factory() {
        return this.factory;
    }

    @External(readonly = true)
    public Address token0() {
        return this.token0;
    }

    @External(readonly = true)
    public Address token1() {
        return this.token1;
    }

    @External(readonly = true)
    public Tick.Info ticks (int tick) {
        return this.ticks.get(tick);
    }

    @External(readonly = true)
    public Position.Info positions (byte[] key) {
        return this.positions.get(key);
    }

    @External(readonly = true)
    public Oracle.Observation observations (int index) {
        return this.observations.get(index);
    }

    @External(readonly = true)
    public Slot0 slot0 () {
        return this.slot0.get();
    }

    @External(readonly = true)
    public ProtocolFees protocolFees () {
        return this.protocolFees.get();
    }

    @External(readonly = true)
    public BigInteger tickBitmap (int index) {
        return this.tickBitmap.get(index);
    }

    @External(readonly = true)
    public BigInteger maxLiquidityPerTick () {
        return this.maxLiquidityPerTick;
    }

    @External(readonly = true)
    public BigInteger liquidity () {
        return this.liquidity.get();
    }

    @External(readonly = true)
    public BigInteger fee () {
        return BigInteger.valueOf(this.fee);
    }

    @External(readonly = true)
    public BigInteger tickSpacing () {
        return BigInteger.valueOf(this.tickSpacing);
    }

    @External(readonly = true)
    public BigInteger feeGrowthGlobal0X128 () {
        return feeGrowthGlobal0X128.get();
    }

    @External(readonly = true)
    public BigInteger feeGrowthGlobal1X128 () {
        return feeGrowthGlobal1X128.get();
    }
}
