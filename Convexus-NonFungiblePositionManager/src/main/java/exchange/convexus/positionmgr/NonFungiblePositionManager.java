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

package exchange.convexus.positionmgr;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.iconloop.score.token.irc721.IRC721;

import exchange.convexus.librairies.FixedPoint128;
import exchange.convexus.librairies.FullMath;
import exchange.convexus.librairies.PairAmounts;
import exchange.convexus.librairies.PoolAddress;
import exchange.convexus.librairies.Position;
import exchange.convexus.librairies.Positions;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

import static exchange.convexus.librairies.BlockTimestamp._blockTimestamp;
import static exchange.convexus.utils.AddressUtils.ZERO_ADDRESS;
import static exchange.convexus.utils.IntUtils.uint128;

import exchange.convexus.liquidity.AddLiquidityParams;
import exchange.convexus.liquidity.ConvexusLiquidityManagement;

// @title NFT positions
// @notice Wraps Convexus positions in the IRC3 non-fungible token interface
public class NonFungiblePositionManager extends IRC721 {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    public static final String NAME = "NonFungiblePositionManager";

    // Contract name
    private final String name;

    // Factory
    private final Address factory;

    // Liquidity Manager
    private final ConvexusLiquidityManagement liquidityMgr;

    // ================================================
    // DB Variables
    // ================================================
    /// @dev IDs of pools assigned by this contract
    private final DictDB<Address, BigInteger> _poolIds = Context.newDictDB(NAME + "_poolIds", BigInteger.class);

    /// @dev Pool keys by pool ID, to save on SSTOREs for position data
    private final DictDB<BigInteger, PoolAddress.PoolKey> _poolIdToPoolKey = Context.newDictDB(NAME + "_poolIdToPoolKey", PoolAddress.PoolKey.class);

    /// @dev The token ID position data
    private final DictDB<BigInteger, NFTPosition> _positions = Context.newDictDB(NAME + "_positions", NFTPosition.class);

    /// @dev The ID of the next token that will be minted. Skips 0
    private final VarDB<BigInteger> _nextId = Context.newVarDB(NAME + "_nextId", BigInteger.class);
    /// @dev The ID of the next pool that is used for the first time. Skips 0
    private final VarDB<BigInteger> _nextPoolId = Context.newVarDB(NAME + "_nextPoolId", BigInteger.class);

    /// @dev The address of the token descriptor contract, which handles generating token URIs for position tokens
    private final Address _tokenDescriptor;

    // ================================================
    // Event Logs
    // ================================================ 
    /// @notice Emitted when liquidity is increased for a position NFT
    /// @dev Also emitted when a token is minted
    /// @param tokenId The ID of the token for which liquidity was increased
    /// @param liquidity The amount by which liquidity for the NFT position was increased
    /// @param amount0 The amount of token0 that was paid for the increase in liquidity
    /// @param amount1 The amount of token1 that was paid for the increase in liquidity
    @EventLog
    protected void IncreaseLiquidity(BigInteger tokenId, BigInteger liquidity, BigInteger amount0, BigInteger amount1) {}
    
    /// @notice Emitted when liquidity is decreased for a position NFT
    /// @param tokenId The ID of the token for which liquidity was decreased
    /// @param liquidity The amount by which liquidity for the NFT position was decreased
    /// @param amount0 The amount of token0 that was accounted for the decrease in liquidity
    /// @param amount1 The amount of token1 that was accounted for the decrease in liquidity
    @EventLog
    protected void DecreaseLiquidity(BigInteger tokenId, BigInteger liquidity, BigInteger amount0, BigInteger amount1) {}
    
    /// @notice Emitted when tokens are collected for a position NFT
    /// @dev The amounts reported may not be exactly equivalent to the amounts transferred, due to rounding behavior
    /// @param tokenId The ID of the token for which underlying tokens were collected
    /// @param recipient The address of the account that received the collected tokens
    /// @param amount0 The amount of token0 owed to the position that was collected
    /// @param amount1 The amount of token1 owed to the position that was collected
    @EventLog
    protected void Collect(BigInteger tokenId, Address recipient, BigInteger amount0Collect, BigInteger amount1Collect) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  @notice Contract constructor
     */
    public NonFungiblePositionManager(
        Address _factory,
        Address _tokenDescriptor_
    ) {
        super("Convexus Positions NFT-V1", "CXS-POS");

        this.liquidityMgr = new ConvexusLiquidityManagement(_factory);
        this.name = "Convexus Position Manager";
        this.factory = _factory;
        this._tokenDescriptor = _tokenDescriptor_;

        if (this._nextId.get() == null) {
            this._nextId.set(ONE);
        }
        if (this._nextPoolId.get() == null) {
            this._nextPoolId.set(ONE);
        }
    }

    /**
     * @notice Returns the position information associated with a given token ID.
     * @dev Throws if the token ID is not valid.
     * @param tokenId The ID of the token that represents the position
     * @return the position
     */
    @External(readonly = true)
    public PositionInformation positions (BigInteger tokenId) {
        NFTPosition position = this._positions.get(tokenId);
        Context.require(position != null, "Invalid token ID");

        PoolAddress.PoolKey poolKey = this._poolIdToPoolKey.get(position.poolId);
        return new PositionInformation(
            position.nonce,
            position.operator,
            poolKey.token0,
            poolKey.token1,
            poolKey.fee,
            position.tickLower,
            position.tickUpper,
            position.liquidity,
            position.feeGrowthInside0LastX128,
            position.feeGrowthInside1LastX128,
            position.tokensOwed0,
            position.tokensOwed1
        );
    }
    
    /// @dev Caches a pool key
    private BigInteger cachePoolKey (Address pool, PoolAddress.PoolKey poolKey) {
        BigInteger poolId = this._poolIds.get(pool);
        if (poolId == null) {
            // increment poolId
            poolId = this._nextPoolId.get();
            this._nextPoolId.set(poolId.add(ONE));

            this._poolIds.set(pool, poolId);
            this._poolIdToPoolKey.set(poolId, poolKey);
        }

        return poolId;
    }

    @External
    public MintResult mint (MintParams params) {
        this.checkDeadline(params.deadline);
        var result = this.liquidityMgr.addLiquidity(new AddLiquidityParams(
            params.token0,
            params.token1,
            params.fee,
            Context.getAddress(),
            params.tickLower,
            params.tickUpper,
            params.amount0Desired,
            params.amount1Desired,
            params.amount0Min,
            params.amount1Min
        ));

        BigInteger liquidity = result.liquidity;
        BigInteger amount0 = result.amount0;
        BigInteger amount1 = result.amount1;
        Address pool = result.pool;

        // increment ID
        BigInteger tokenId = this._nextId.get();
        this._nextId.set(tokenId.add(ONE));
        this._mint(params.recipient, tokenId);

        var positionKey = Positions.getKey(Context.getAddress(), params.tickLower, params.tickUpper);
        var poolPos = Position.Info.fromCall(Context.call(pool, "positions", positionKey));
        
        // idempotent set
        BigInteger poolId = cachePoolKey(pool, PoolAddress.getPoolKey(params.token0, params.token1, params.fee));

        this._positions.set(tokenId, new NFTPosition(
            ZERO,
            ZERO_ADDRESS,
            poolId,
            params.tickLower,
            params.tickUpper,
            liquidity,
            poolPos.feeGrowthInside0LastX128,
            poolPos.feeGrowthInside1LastX128,
            ZERO,
            ZERO
        ));

        this.IncreaseLiquidity(tokenId, liquidity, amount0, amount1);

        return new MintResult(tokenId, liquidity, amount0, amount1);
    }

    @External(readonly = true)
    public String tokenURI (BigInteger tokenId) {
        _exists(tokenId);
        return (String) Context.call(this._tokenDescriptor, "tokenURI", Context.getAddress(), tokenId);
    }

    /**
     * @notice Increases the amount of liquidity in a position, with tokens paid by the `msg.sender`
     * @param params tokenId The ID of the token for which liquidity is being increased,
     * amount0Desired The desired amount of token0 to be spent,
     * amount1Desired The desired amount of token1 to be spent,
     * amount0Min The minimum amount of token0 to spend, which serves as a slippage check,
     * amount1Min The minimum amount of token1 to spend, which serves as a slippage check,
     * deadline The time by which the transaction must be included to effect the change
     * @return liquidity The new liquidity amount as a result of the increase
     * @return amount0 The amount of token0 to acheive resulting liquidity
     * @return amount1 The amount of token1 to acheive resulting liquidity
     */
    @External
    public IncreaseLiquidityResult increaseLiquidity (IncreaseLiquidityParams params) {
        this.checkDeadline(params.deadline);

        NFTPosition positionStorage = this._positions.get(params.tokenId);
        PoolAddress.PoolKey poolKey = this._poolIdToPoolKey.get(positionStorage.poolId);

        var result = this.liquidityMgr.addLiquidity(new AddLiquidityParams(
            poolKey.token0,
            poolKey.token1,
            poolKey.fee,
            Context.getAddress(),
            positionStorage.tickLower,
            positionStorage.tickUpper,
            params.amount0Desired,
            params.amount1Desired,
            params.amount0Min,
            params.amount1Min
        ));

        BigInteger liquidity = result.liquidity;
        BigInteger amount0 = result.amount0;
        BigInteger amount1 = result.amount1;
        Address pool = result.pool;

        var positionKey = Positions.getKey(Context.getAddress(), positionStorage.tickLower, positionStorage.tickUpper);

        // this is now updated to the current transaction
        var poolPos = Position.Info.fromCall(Context.call(pool, "positions", positionKey));

        BigInteger feeGrowthInside0LastX128 = poolPos.feeGrowthInside0LastX128;
        BigInteger feeGrowthInside1LastX128 = poolPos.feeGrowthInside1LastX128;

        // calculate accumulated fees
        BigInteger tokensOwed0 = uint128(FullMath.mulDiv(feeGrowthInside0LastX128.subtract(positionStorage.feeGrowthInside0LastX128), positionStorage.liquidity, FixedPoint128.Q128));
        BigInteger tokensOwed1 = uint128(FullMath.mulDiv(feeGrowthInside1LastX128.subtract(positionStorage.feeGrowthInside1LastX128), positionStorage.liquidity, FixedPoint128.Q128));

        positionStorage.tokensOwed0 = positionStorage.tokensOwed0.add(tokensOwed0);
        positionStorage.tokensOwed1 = positionStorage.tokensOwed1.add(tokensOwed1);

        positionStorage.feeGrowthInside0LastX128 = feeGrowthInside0LastX128;
        positionStorage.feeGrowthInside1LastX128 = feeGrowthInside1LastX128;
        positionStorage.liquidity = positionStorage.liquidity.add(poolPos.liquidity);

        this._positions.set(params.tokenId, positionStorage);
        
        this.IncreaseLiquidity(params.tokenId, liquidity, amount0, amount1);

        return new IncreaseLiquidityResult(liquidity, amount0, amount1);
    }

    /**
     * @notice Decreases the amount of liquidity in a position and accounts it to the position
     * @param params tokenId The ID of the token for which liquidity is being decreased,
     * amount The amount by which liquidity will be decreased,
     * amount0Min The minimum amount of token0 that should be accounted for the burned liquidity,
     * amount1Min The minimum amount of token1 that should be accounted for the burned liquidity,
     * deadline The time by which the transaction must be included to effect the change
     * @return amount0 The amount of token0 accounted to the position's tokens owed
     * @return amount1 The amount of token1 accounted to the position's tokens owed
     */
    @External
    public PairAmounts decreaseLiquidity (DecreaseLiquidityParams params) {
        this.isAuthorizedForToken(params.tokenId);
        this.checkDeadline(params.deadline);
        Context.require(params.liquidity.compareTo(ZERO) > 0, 
            "decreaseLiquidity: liquidity must be superior to zero");

        BigInteger amount0, amount1;

        NFTPosition positionStorage = this._positions.get(params.tokenId);
        BigInteger positionLiquidity = positionStorage.liquidity;
        Context.require(positionLiquidity.compareTo(params.liquidity) >= 0,
            "decreaseLiquidity: invalid liquidity");

        PoolAddress.PoolKey poolKey = this._poolIdToPoolKey.get(positionStorage.poolId);
        Address pool = PoolAddress.getPool(this.factory, poolKey);
        var pairAmounts = (PairAmounts) Context.call(pool, "burn", positionStorage.tickLower, positionStorage.tickUpper, positionStorage.liquidity);
        amount0 = pairAmounts.amount0;
        amount1 = pairAmounts.amount1;

        Context.require(amount0.compareTo(params.amount0Min) >= 0 && amount1.compareTo(params.amount1Min) >= 0,
            "decreaseLiquidity: Price slippage check");

        var positionKey = Positions.getKey(Context.getAddress(), positionStorage.tickLower, positionStorage.tickUpper);
        // this is now updated to the current transaction
        var poolPos = Position.Info.fromCall(Context.call(pool, "positions", positionKey));

        BigInteger feeGrowthInside0LastX128 = poolPos.feeGrowthInside0LastX128;
        BigInteger feeGrowthInside1LastX128 = poolPos.feeGrowthInside1LastX128;

        // calculate accumulated fees
        BigInteger tokensOwed0 = uint128(amount0).add(uint128(FullMath.mulDiv(feeGrowthInside0LastX128.subtract(positionStorage.feeGrowthInside0LastX128), positionLiquidity, FixedPoint128.Q128)));
        BigInteger tokensOwed1 = uint128(amount1).add(uint128(FullMath.mulDiv(feeGrowthInside1LastX128.subtract(positionStorage.feeGrowthInside1LastX128), positionLiquidity, FixedPoint128.Q128)));

        positionStorage.tokensOwed0 = positionStorage.tokensOwed0.add(tokensOwed0);
        positionStorage.tokensOwed1 = positionStorage.tokensOwed1.add(tokensOwed1);

        positionStorage.feeGrowthInside0LastX128 = feeGrowthInside0LastX128;
        positionStorage.feeGrowthInside1LastX128 = feeGrowthInside1LastX128;
        // subtraction is safe because we checked positionLiquidity is gte params.liquidity
        positionStorage.liquidity = positionLiquidity.subtract(params.liquidity);

        this.DecreaseLiquidity(params.tokenId, params.liquidity, amount0, amount1);

        this._positions.set(params.tokenId, positionStorage);
        return new PairAmounts(amount0, amount1);
    }

    @External
    public PairAmounts collect (CollectParams params) {
        isAuthorizedForToken(params.tokenId);

        Context.require(params.amount0Max.compareTo(ZERO) > 0 || params.amount1Max.compareTo(ZERO) > 0);
        // allow collecting to the nft position manager address with address 0
        Address recipient = params.recipient == ZERO_ADDRESS ? Context.getAddress() : params.recipient;

        NFTPosition positionStorage = this._positions.get(params.tokenId);
        PoolAddress.PoolKey poolKey = this._poolIdToPoolKey.get(positionStorage.poolId);
        Address pool = PoolAddress.getPool(this.factory, poolKey);

        BigInteger tokensOwed0 = positionStorage.tokensOwed0;
        BigInteger tokensOwed1 = positionStorage.tokensOwed1;

        // trigger an update of the position fees owed and fee growth snapshots if it has any liquidity
        if (positionStorage.liquidity.compareTo(ZERO) > 0) {
            Context.call(pool, "burn", positionStorage.tickLower, positionStorage.tickUpper, ZERO);
            var positionKey = Positions.getKey(Context.getAddress(), positionStorage.tickLower, positionStorage.tickUpper);
            var poolPos = Position.Info.fromCall(Context.call(pool, "positions", positionKey));
            BigInteger feeGrowthInside0LastX128 = poolPos.feeGrowthInside0LastX128;
            BigInteger feeGrowthInside1LastX128 = poolPos.feeGrowthInside1LastX128;

            // calculate accumulated fees
            tokensOwed0 = tokensOwed0.add(uint128(FullMath.mulDiv(feeGrowthInside0LastX128.subtract(positionStorage.feeGrowthInside0LastX128), positionStorage.liquidity, FixedPoint128.Q128)));
            tokensOwed1 = tokensOwed1.add(uint128(FullMath.mulDiv(feeGrowthInside1LastX128.subtract(positionStorage.feeGrowthInside1LastX128), positionStorage.liquidity, FixedPoint128.Q128)));
                
            positionStorage.feeGrowthInside0LastX128 = feeGrowthInside0LastX128;
            positionStorage.feeGrowthInside1LastX128 = feeGrowthInside1LastX128;
        }
        
        // compute the arguments to give to the pool#collect method
        BigInteger amount0Collect, amount1Collect;
        amount0Collect = (params.amount0Max.compareTo(tokensOwed0) > 0) ? tokensOwed0 : params.amount0Max;
        amount1Collect = (params.amount1Max.compareTo(tokensOwed1) > 0) ? tokensOwed1 : params.amount1Max;
        
        // the actual amounts collected are returned
        var result = (PairAmounts) Context.call(pool, "collect", 
            recipient,
            positionStorage.tickLower,
            positionStorage.tickUpper,
            amount0Collect,
            amount1Collect
        );
        BigInteger amount0 = result.amount0;
        BigInteger amount1 = result.amount1;
        
        // sometimes there will be a few less loops than expected due to rounding down in core, but we just subtract the full amount expected
        // instead of the actual amount so we can burn the token
        positionStorage.tokensOwed0 = tokensOwed0.subtract(amount0Collect);
        positionStorage.tokensOwed1 = tokensOwed1.subtract(amount1Collect);

        this.Collect(params.tokenId, recipient, amount0Collect, amount1Collect);
        
        this._positions.set(params.tokenId, positionStorage);
        return new PairAmounts(amount0, amount1);
    }


    /**
     * @notice Burns a token ID, which deletes it from the NFT contract. 
     * The token must have 0 liquidity and all tokens must be collected first.
     * @param tokenId The ID of the token that is being burned
     */
    @External
    public void burn (BigInteger tokenId) {
        isAuthorizedForToken(tokenId);

        NFTPosition positionStorage = this._positions.get(tokenId);

        Context.require(
           positionStorage.liquidity.equals(ZERO) 
        && positionStorage.tokensOwed0.equals(ZERO)
        && positionStorage.tokensOwed1.equals(ZERO),
            "burn: Not cleared"
        );

        this._positions.set(tokenId, null);
        this._burn(tokenId);
    }

    // ================================================
    // Overrides
    // ================================================
    /// @dev Overrides _approve to use the operator in the position, which is packed with the position permit nonce
    @Override
    protected void _approve (Address to, BigInteger tokenId) {
        var position = this._positions.get(tokenId);
        position.operator = to;
        this._positions.set(tokenId, position);
        this.Approval(ownerOf(tokenId), to, tokenId);
    }

    @Override
    @External(readonly=true)
    public Address getApproved(BigInteger tokenId) {
        Context.require(this._exists(tokenId), 
            "getApproved: approved query for nonexistent token");
        return this._positions.get(tokenId).operator;
    }

    // ================================================
    // Checks
    // ================================================
    /**
     * Check if transaction hasn't reached the deadline
     * @param deadline
     */
    private void checkDeadline(BigInteger deadline) {
        Context.require(_blockTimestamp().compareTo(deadline) <= 0,
            "checkDeadline: Transaction too old");
    }

    private void isAuthorizedForToken (BigInteger tokenId) {
        Context.require(_isApprovedOrOwner(Context.getCaller(), tokenId), 
            "checkAuthorizedForToken: Not approved");
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
