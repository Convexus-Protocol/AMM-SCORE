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

package exchange.switchy.pool;

import java.math.BigInteger;

import exchange.switchy.common.SwitchyPoolDeployerParameters;
import exchange.switchy.librairies.LiquidityMath;
import exchange.switchy.librairies.Oracle;
import exchange.switchy.librairies.Position;
import exchange.switchy.librairies.Positions;
import exchange.switchy.librairies.SqrtPriceMath;
import exchange.switchy.librairies.Tick;
import exchange.switchy.librairies.TickBitmap;
import exchange.switchy.librairies.TickMath;
import exchange.switchy.librairies.Ticks;
import exchange.switchy.utils.ReentrancyLock;
import exchange.switchy.utils.TimeUtils;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

class Slot0 {
    // The current price of the pool as a sqrt(token1/token0) Q64.96 value
    BigInteger sqrtPriceX96;
    // The current tick of the pool, i.e. according to the last tick transition that was run.
    // This value may not always be equal to SqrtTickMath.getTickAtSqrtRatio(sqrtPriceX96) if the price is on a tick boundary
    int tick;
    // The index of the last oracle observation that was written
    int observationIndex;
    // the current maximum number of observations that are being stored
    int observationCardinality;
    // the next maximum number of observations to store, triggered in observations.write
    int observationCardinalityNext;
    // The current protocol fee as a percentage of the swap fee taken on withdrawal
    // represented as an integer denominator (1/x)%
    // Encoded as two 4 bit values, where the protocol fee of token1 is shifted 4 bits and the protocol fee of token0
    // is the lower 4 bits. Used as the denominator of a fraction of the swap fee, e.g. 4 means 1/4th of the swap fee.
    int feeProtocol;

    public Slot0 (
        BigInteger sqrtPriceX96,
        int tick,
        int observationIndex,
        int observationCardinality,
        int observationCardinalityNext,
        int feeProtocol
    ) {
        this.sqrtPriceX96 = sqrtPriceX96;
        this.tick = tick;
        this.observationIndex = observationIndex;
        this.observationCardinality = observationCardinality;
        this.observationCardinalityNext = observationCardinalityNext;
        this.feeProtocol = feeProtocol;
    }
}

// accumulated protocol fees in token0/token1 units
class ProtocolFees {
    BigInteger token0;
    BigInteger token1;
}

public class SwitchyPool {

    // ================================================
    // Consts
    // ================================================
    
    // Contract class name
    private static final String NAME = "SwitchyPool";

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
    private final VarDB<BigInteger> feeGrowthGlobal0X128 = Context.newVarDB(NAME + "_feeGrowthGlobal0X128", BigInteger.class);
    // The fee growth as a Q128.128 fees of token1 collected per unit of liquidity for the entire life of the pool
    private final VarDB<BigInteger> feeGrowthGlobal1X128 = Context.newVarDB(NAME + "_feeGrowthGlobal1X128", BigInteger.class);
    
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
    private final Oracle.Observations observations = new Oracle.Observations();


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
  
    @EventLog
    protected void Initialize(
        BigInteger sqrtPriceX96,
        int tick
    ) {}
    
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

    @EventLog(indexed = 3)
    protected void Collect(
        Address caller, 
        int tickLower, 
        int tickUpper, 
        Address recipient, 
        BigInteger amount0,
        BigInteger amount1
    ) {}
  
    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     *  
     */
    public SwitchyPool() {
        SwitchyPoolDeployerParameters parameters = (SwitchyPoolDeployerParameters) Context.call(Context.getCaller(), "parameters");
        this.factory = parameters.factory;
        this.token0 = parameters.token0;
        this.token1 = parameters.token1;
        this.fee = parameters.fee;
        this.tickSpacing = parameters.tickSpacing;

        this.maxLiquidityPerTick = Tick.tickSpacingToMaxLiquidityPerTick(parameters.tickSpacing);

        this.name = "SwitchyPool " + this.token0 + "-" + this.token1;
        
        // locked by default
        this.poolLock.lock(true);
    }

    private void checkTicks (int tickLower, int tickUpper) {
        Context.require(tickLower < tickUpper, "checkTicks: TLU");
        Context.require(tickLower >= TickMath.MIN_TICK, "checkTicks: TLM");
        Context.require(tickUpper <= TickMath.MAX_TICK, "checkTicks: TUM");
    }

    /**
     * Returns the block timestamp truncated to seconds.
     */
    private BigInteger _blockTimestamp () {
        return TimeUtils.nowSeconds();
    }

    /**
     * @dev Get the pool's balance of token0
     */
    private BigInteger balance0 () {
        return (BigInteger) Context.call(this.token0, "balanceOf", Context.getAddress());
    }

    /**
     * @dev Get the pool's balance of token1
     */
    private BigInteger balance1 () {
        return (BigInteger) Context.call(this.token1, "balanceOf", Context.getAddress());
    }
    
    /**
     * @notice Returns a snapshot of the tick cumulative, seconds per liquidity and seconds inside a tick range
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
            Oracle.Observations.ObserveSingleResult result = observations.observeSingle(
                time, 
                BigInteger.ZERO, 
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
    public Oracle.Observations.ObserveResult observe (BigInteger[] secondsAgos) {
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
     * @dev Price is represented as a sqrt(amountToken1/amountToken0) Q64.96 value
     * @param sqrtPriceX96 the initial sqrt price of the pool as a Q64.96
     * @dev not locked because it initializes unlocked
     */
    @External
    public void initialize (BigInteger sqrtPriceX96) {
        Slot0 _slot0 = this.slot0.get();

        Context.require(_slot0.sqrtPriceX96.equals(BigInteger.ZERO), 
            "initialize: sqrtPriceX96 must be equal to 0");

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
    private Position.Info _updatePosition (
        Address owner,
        int tickLower,
        int tickUpper,
        BigInteger liquidityDelta,
        int tick
    ) {
        byte[] positionKey = Positions.getKey(owner, tickLower, tickUpper);
        Position.Info position = this.positions.get(positionKey);

        BigInteger _feeGrowthGlobal0X128 = feeGrowthGlobal0X128.get();
        BigInteger _feeGrowthGlobal1X128 = feeGrowthGlobal1X128.get();
        Slot0 _slot0 = this.slot0.get();
        
        // if we need to update the ticks, do it
        boolean flippedLower = false;
        boolean flippedUpper = false;
        if (!liquidityDelta.equals(BigInteger.ZERO)) {
            BigInteger time = _blockTimestamp();
            var result = this.observations.observeSingle(
                time, 
                BigInteger.ZERO, 
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
        if (liquidityDelta.compareTo(BigInteger.ZERO) < 0) {
            if (flippedLower) {
                ticks.clear(tickLower);
            }
            if (flippedUpper) {
                ticks.clear(tickUpper);
            }
        }

        this.positions.set(positionKey, position);
        return position;
    }

    class ModifyPositionParams {
        // the address that owns the position
        public Address owner;
        // the lower and upper tick of the position
        public int tickLower;
        public int tickUpper;
        // any change in liquidity
        public BigInteger liquidityDelta;
        
        public ModifyPositionParams(Address recipient, int tickLower, int tickUpper, BigInteger amount) {
            this.owner = recipient;
            this.tickLower = tickLower;
            this.tickUpper = tickUpper;
            this.liquidityDelta = amount;
        }
    }

    class ModifyPositionResult {
        public Position.Info position;
        public BigInteger amount0;
        public BigInteger amount1;
        
        public ModifyPositionResult (Position.Info position, BigInteger amount0, BigInteger amount1) {
            this.position = position;
            this.amount0 = amount0;
            this.amount1 = amount1;
        }
    }

    private ModifyPositionResult _modifyPosition (ModifyPositionParams params) {
        checkTicks(params.tickLower, params.tickUpper);

        Slot0 _slot0 = this.slot0.get();

        Position.Info position = _updatePosition(
            params.owner,
            params.tickLower,
            params.tickUpper,
            params.liquidityDelta,
            _slot0.tick
        );

        BigInteger amount0 = BigInteger.ZERO;
        BigInteger amount1 = BigInteger.ZERO;

        if (!params.liquidityDelta.equals(BigInteger.ZERO)) {
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

                liquidity.set(LiquidityMath.addDelta(liquidityBefore, params.liquidityDelta));
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

        return new ModifyPositionResult(position, amount0, amount1);
    }

    class PairAmounts {
        public PairAmounts(BigInteger amount0, BigInteger amount1) {
            this.amount0 = amount0;
            this.amount1 = amount1;
        }
        public BigInteger amount0;
        public BigInteger getamount0() { return this.amount0; }
        public void setamount0(BigInteger v) { this.amount0 = v; }
        public BigInteger amount1;
        public BigInteger getamount1() { return this.amount1; }
        public void setamount1(BigInteger v) { this.amount1 = v; }
    }

    @External
    public PairAmounts mint (
        Address recipient,
        int tickLower,
        int tickUpper,
        BigInteger amount
    ) {
        this.poolLock.lock(true);

        final Address caller = Context.getCaller();

        Context.require(amount.compareTo(BigInteger.ZERO) > 0,
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

        BigInteger balance0Before = BigInteger.ZERO;
        BigInteger balance1Before = BigInteger.ZERO;
        
        if (amount0.compareTo(BigInteger.ZERO) > 0) {
            balance0Before = balance0();
        }
        if (amount1.compareTo(BigInteger.ZERO) > 0) {
            balance1Before = balance1();
        }

        Context.call(caller, "uniswapV3MintCallback", amount0, amount1);
        
        if (amount0.compareTo(BigInteger.ZERO) > 0) {
            Context.require(balance0Before.add(amount0).compareTo(balance0()) <= 0, 
            "mint: M0");
        }
        if (amount1.compareTo(BigInteger.ZERO) > 0) {
            Context.require(balance1Before.add(amount1).compareTo(balance1()) <= 0, 
            "mint: M1");
        }

        this.Mint(recipient, tickLower, tickUpper, caller, amount, amount0, amount1);

        this.poolLock.lock(false);
        return new PairAmounts(amount0, amount1);
    }

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

        if (amount0.compareTo(BigInteger.ZERO) > 0) {
            position.tokensOwed0 = position.tokensOwed0.subtract(amount0);
            this.positions.set(key, position);
            Context.call(this.token0, "transfer", recipient, amount0);
        }
        if (amount1.compareTo(BigInteger.ZERO) > 0) {
            position.tokensOwed1 = position.tokensOwed1.subtract(amount1);
            this.positions.set(key, position);
            Context.call(this.token1, "transfer", recipient, amount1);
        }

        this.Collect(caller, tickLower, tickUpper, recipient, amount0, amount1);

        this.poolLock.lock(false);
        return new PairAmounts(amount0, amount1);
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly=true)
    public String name() {
        return this.name;
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

    // ================================================
    // Admin methods
    // ================================================
}
