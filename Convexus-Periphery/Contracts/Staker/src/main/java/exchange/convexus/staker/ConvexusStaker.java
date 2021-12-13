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

package exchange.convexus.staker;

import static exchange.convexus.utils.AddressUtils.ZERO_ADDRESS;
import static exchange.convexus.utils.IntUtils.MAX_UINT96;
import static exchange.convexus.utils.IntUtils.uint96;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import exchange.convexus.pool.SnapshotCumulativesInsideResult;
import exchange.convexus.positionmgr.PositionInformation;
import exchange.convexus.utils.StringUtils;
import static exchange.convexus.utils.TimeUtils.now;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.ObjectReader;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import scorex.io.Reader;
import scorex.io.StringReader;

/**
 * @title Convexus canonical staking interface
 */
public class ConvexusStaker {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    private static final String NAME = "ConvexusStaker";

    // Contract name
    private final String name;
    // Convexus Factory
    private final Address factory;
    // The nonfungible position manager with which this staking contract is compatible
    private final Address nonfungiblePositionManager;
    // The max duration of an incentive in seconds
    private final BigInteger maxIncentiveStartLeadTime;
    // The max amount of seconds into the future the incentive startTime can be set
    private final BigInteger maxIncentiveDuration;

    // ================================================
    // DB Variables
    // ================================================
    /// @dev bytes[] refers to the return value of IncentiveId.compute
    private final DictDB<byte[], Incentive> incentives = Context.newDictDB(NAME + "_incentives", Incentive.class);

    /// @dev deposits[tokenId] => Deposit
    private final DictDB<BigInteger, Deposit> deposits = Context.newDictDB(NAME + "_deposits", Deposit.class);

    /// @dev stakes[tokenId][incentiveHash] => Stake
    private final BranchDB<BigInteger, DictDB<byte[], Stake>> _stakes = Context.newBranchDB(NAME + "_stakes", Stake.class);

    /// @dev rewards[rewardToken][owner] => uint256
    private final BranchDB<Address, DictDB<Address, BigInteger>> rewards = Context.newBranchDB(NAME + "_rewards", BigInteger.class);

    // ================================================
    // EventLogs
    // ================================================
    /// @notice Event emitted when a liquidity mining incentive has been created
    /// @param rewardToken The token being distributed as a reward
    /// @param pool The Convexus pool
    /// @param startTime The time when the incentive program begins
    /// @param endTime The time when rewards stop accruing
    /// @param refundee The address which receives any remaining reward tokens after the end time
    /// @param reward The amount of reward tokens to be distributed
    @EventLog
    protected void IncentiveCreated(
        Address rewardToken, 
        Address pool, 
        BigInteger startTime, 
        BigInteger endTime,
        Address refundee, 
        BigInteger reward) {}

    /// @notice Event that can be emitted when a liquidity mining incentive has ended
    /// @param incentiveId The incentive which is ending
    /// @param refund The amount of reward tokens refunded
    @EventLog
    protected void IncentiveEnded(
        byte[] incentiveId, 
        BigInteger refund) {}

    /// @notice Emitted when ownership of a deposit changes
    /// @param tokenId The ID of the deposit (and token) that is being transferred
    /// @param oldOwner The owner before the deposit was transferred
    /// @param newOwner The owner after the deposit was transferred
    @EventLog
    protected void DepositTransferred(
        BigInteger tokenId, 
        Address oldOwner, 
        Address newOwner) {}
    
    /// @notice Event emitted when a Uniswap V3 LP token has been unstaked
    /// @param tokenId The unique identifier of an Uniswap V3 LP token
    /// @param incentiveId The incentive in which the token is staking
    @EventLog
    protected void TokenUnstaked(BigInteger tokenId, byte[] incentiveId) {}

    /// @notice Event emitted when a reward token has been claimed
    /// @param to The address where claimed rewards were sent to
    /// @param reward The amount of reward tokens claimed
    @EventLog
    protected void RewardClaimed(Address to, BigInteger reward) {}

    /// @notice Event emitted when a Uniswap V3 LP token has been staked
    /// @param tokenId The unique identifier of an Uniswap V3 LP token
    /// @param liquidity The amount of liquidity staked
    /// @param incentiveId The incentive in which the token is staking
    @EventLog
    protected void TokenStaked(BigInteger tokenId, byte[] incentiveId, BigInteger liquidity) {}

    // ================================================
    // Methods
    // ================================================
    /**
     * Contract constructor
     * @param _factory The Convexus factory
     * @param _nonfungiblePositionManager the NFT position manager contract address
     * @param _maxIncentiveStartLeadTime the max duration of an incentive in seconds
     * @param _maxIncentiveDuration the max amount of seconds into the future the incentive startTime can be set
     */
    public ConvexusStaker (
        Address _factory,
        Address _nonfungiblePositionManager,
        BigInteger _maxIncentiveStartLeadTime,
        BigInteger _maxIncentiveDuration
    ) {
        this.name = "Convexus Staker";
        this.factory = _factory;
        this.nonfungiblePositionManager = _nonfungiblePositionManager;
        this.maxIncentiveStartLeadTime = _maxIncentiveStartLeadTime;
        this.maxIncentiveDuration = _maxIncentiveDuration;
    }

    /**
     * @notice Creates a new liquidity mining incentive program
     * @param key Details of the incentive to create
     * @param reward The amount of reward tokens to be distributed
     * @dev @External public through tokenFallback
     */
    private void createIncentive (IncentiveKey key, BigInteger reward) {
        final BigInteger now = now();

        Context.require(reward.compareTo(ZERO) > 0,
            "createIncentive: reward must be positive");

        Context.require(now.compareTo(key.startTime) <= 0,
            "createIncentive: start time must be now or in the future");

        Context.require(key.startTime.subtract(now).compareTo(maxIncentiveStartLeadTime) <= 0,
            "createIncentive: start time too far into future");

        Context.require(key.startTime.compareTo(key.endTime) < 0,
            "createIncentive: start time must be before end time");

        Context.require(key.endTime.subtract(key.startTime).compareTo(maxIncentiveDuration) <= 0,
            "createIncentive: incentive duration is too long");

        byte[] incentiveId = IncentiveId.compute(key);

        var incentive = this.incentives.getOrDefault(incentiveId, Incentive.empty());
        incentive.totalRewardUnclaimed = incentive.totalRewardUnclaimed.add(reward);

        this.incentives.set(incentiveId, incentive);

        this.IncentiveCreated(key.rewardToken, key.pool, key.startTime, key.endTime, key.refundee, reward);
    }

    /**
     * @notice Ends an incentive after the incentive end time has passed and all stakes have been withdrawn
     * @param key Details of the incentive to end
     * @return refund The remaining reward tokens when the incentive is ended
     */
    @External
    public BigInteger endIncentive (IncentiveKey key) {
        final BigInteger now = now();
        Context.require(now.compareTo(key.endTime) >= 0,
            "endIncentive: cannot end incentive before end time");

        byte[] incentiveId = IncentiveId.compute(key);
        Incentive incentiveStorage = this.incentives.getOrDefault(incentiveId, Incentive.empty());

        BigInteger refund = incentiveStorage.totalRewardUnclaimed;
        Context.require(refund.compareTo(ZERO) > 0,
            "endIncentive: no refund available");

        Context.require(incentiveStorage.numberOfStakes.equals(ZERO),
            "endIncentive: cannot end incentive while deposits are staked");

        // issue the refund
        incentiveStorage.totalRewardUnclaimed = ZERO;
        this.incentives.set(incentiveId, incentiveStorage);

        Context.call(key.rewardToken, "transfer", key.refundee, refund, "refund".getBytes());

        // @dev we never clear totalSecondsClaimedX128

        this.IncentiveEnded(incentiveId, refund);
        return refund;
    }
    
    /**
     * @notice Upon receiving a Uniswap V3 ERC721, creates the token deposit setting owner to `from`. Also stakes token
     * in one or more incentives if properly formatted `data` has a length > 0.
     */
    @External
    public void onIRC721Received (Address caller, Address from, BigInteger tokenId, byte[] data) {
        Context.require(Context.getCaller().equals(this.nonfungiblePositionManager),
            "onIRC721Received: not a Convexus NFT");
        
        var position = PositionInformation.fromMap(Context.call(this.nonfungiblePositionManager, "positions", tokenId));
        var deposit = new Deposit(from, ZERO, position.tickLower, position.tickUpper);
        this.deposits.set(tokenId, deposit);

        this.DepositTransferred(tokenId, ZERO_ADDRESS, from);

        if (data.length > 0) {
            ObjectReader reader = Context.newByteArrayObjectReader("RLPn", data);
            reader.beginList();
            while (reader.hasNext()) {
                _stakeToken(reader.read(IncentiveKey.class), tokenId);
            }
            reader.end();
        }
    }

    /// @notice Transfers ownership of a deposit from the sender to the given recipient
    /// @param tokenId The ID of the token (and the deposit) to transfer
    /// @param to The new owner of the deposit
    @External
    public void transferDeposit(BigInteger tokenId, Address to) {
        Context.require(!to.equals(ZERO_ADDRESS), "transferDeposit: invalid transfer recipient");
        Address owner = deposits.get(tokenId).owner;
        Context.require(owner.equals(Context.getCaller()), "transferDeposit: can only be called by deposit owner");
        var deposit = deposits.get(tokenId);
        deposit.owner = to;
        deposits.set(tokenId, deposit);
        this.DepositTransferred(tokenId, owner, to);
    }

    /// @notice Withdraws a Convexus LP token `tokenId` from this contract to the recipient `to`
    /// @param tokenId The unique identifier of an Convexus LP token
    /// @param to The address where the LP token will be sent
    /// @param data An optional data array that will be passed along to the `to` address via the NFT safeTransferFrom
    @External
    public void withdrawToken (
        BigInteger tokenId,
        Address to,
        byte[] data
    ) {
        final Address thisAddress = Context.getAddress();
        final Address caller = Context.getCaller();

        Context.require(!to.equals(thisAddress), 
            "withdrawToken: cannot withdraw to staker");

        Deposit deposit = deposits.get(tokenId);

        Context.require(deposit.numberOfStakes.equals(ZERO), 
            "withdrawToken: cannot withdraw token while staked");

        Context.require(deposit.owner.equals(caller), 
            "withdrawToken: only owner can withdraw token");

        this.deposits.set(tokenId, null);
        this.DepositTransferred(tokenId, deposit.owner, ZERO_ADDRESS);

        Context.call(nonfungiblePositionManager, "safeTransferFrom", thisAddress, to, tokenId, data);
    }

    /// @notice Stakes a Uniswap V3 LP token
    /// @param key The key of the incentive for which to stake the NFT
    /// @param tokenId The ID of the token to stake
    @External
    public void stakeToken(IncentiveKey key, BigInteger tokenId) {
        final Address caller = Context.getCaller();
        Context.require(deposits.get(tokenId).owner.equals(caller),
            "stakeToken: only owner can stake token");

        _stakeToken(key, tokenId);
    }

    /// @notice Unstakes a Uniswap V3 LP token
    /// @param key The key of the incentive for which to unstake the NFT
    /// @param tokenId The ID of the token to unstake
    @External
    public void unstakeToken (IncentiveKey key, BigInteger tokenId) {
        Deposit deposit = deposits.get(tokenId);
        final BigInteger now = now();
        final Address caller = Context.getCaller();

        // anyone can call unstakeToken if the block time is after the end time of the incentive
        if (now.compareTo(key.endTime) < 0) {
            Context.require(deposit.owner.equals(caller),
                "unstakeToken: only owner can withdraw token before incentive end time");
        }

        byte[] incentiveId = IncentiveId.compute(key);

        var stakes = stakes(tokenId, incentiveId);
        BigInteger secondsPerLiquidityInsideInitialX128 = stakes.secondsPerLiquidityInsideInitialX128;
        BigInteger liquidity = stakes.liquidity;
        
        Context.require(liquidity.compareTo(ZERO) > 0,
            "unstakeToken: stake does not exist");

        Incentive incentive = incentives.get(incentiveId);

        deposit.numberOfStakes = deposit.numberOfStakes.subtract(ONE);
        deposits.set(tokenId, deposit);
        incentive.numberOfStakes = incentive.numberOfStakes.subtract(ONE);
        incentives.set(incentiveId, incentive);

        var snapshot = SnapshotCumulativesInsideResult.fromMap(Context.call(key.pool, "snapshotCumulativesInside", deposit.tickLower, deposit.tickUpper));
        BigInteger secondsPerLiquidityInsideX128 = snapshot.secondsPerLiquidityInsideX128;

        var result =
            RewardMath.computeRewardAmount(
                incentive.totalRewardUnclaimed,
                incentive.totalSecondsClaimedX128,
                key.startTime,
                key.endTime,
                liquidity,
                secondsPerLiquidityInsideInitialX128,
                secondsPerLiquidityInsideX128,
                now
            );
        
        BigInteger rewardAmount = result.reward;
        BigInteger secondsInsideX128 = result.secondsInsideX128;

        // if this overflows, e.g. after 2^32-1 full liquidity seconds have been claimed,
        // reward rate will fall drastically so it's safe
        incentive.totalSecondsClaimedX128 = incentive.totalSecondsClaimedX128.add(secondsInsideX128);
        // reward is never greater than total reward unclaimed
        incentive.totalRewardUnclaimed = incentive.totalRewardUnclaimed.subtract(rewardAmount);
        incentives.set(incentiveId, incentive);

        // this only overflows if a token has a total supply greater than type(uint256).max
        var rewardToken = rewards.at(key.rewardToken);
        rewardToken.set(deposit.owner, rewardToken.getOrDefault(deposit.owner, ZERO).add(rewardAmount));

        Stake stake = _stakes.at(tokenId).get(incentiveId);
        stake.secondsPerLiquidityInsideInitialX128 = ZERO;
        stake.liquidityNoOverflow = ZERO;
        if (liquidity.compareTo(MAX_UINT96) >= 0) {
            stake.liquidityIfOverflow = ZERO;
        }
        _stakes.at(tokenId).set(incentiveId, stake);

        this.TokenUnstaked(tokenId, incentiveId);
    }

    /// @notice Transfers `amountRequested` of accrued `rewardToken` rewards from the contract to the recipient `to`
    /// @param rewardToken The token being distributed as a reward
    /// @param to The address where claimed rewards will be sent to
    /// @param amountRequested The amount of reward tokens to claim. Claims entire reward amount if set to 0.
    /// @return reward The amount of reward tokens claimed
    @External
    public BigInteger claimReward (
        Address rewardToken,
        Address to,
        BigInteger amountRequested
    ) {
        final Address caller = Context.getCaller();
        BigInteger reward = rewards.at(rewardToken).getOrDefault(caller, ZERO);

        if (!amountRequested.equals(ZERO) 
        &&  amountRequested.compareTo(reward) < 0) {
            reward = amountRequested;
        }

        var rewardTokenDict = rewards.at(rewardToken);
        rewardTokenDict.set(caller, rewardTokenDict.get(caller).subtract(reward));
        Context.call(rewardToken, "transfer", to, reward, "claimReward".getBytes());

        this.RewardClaimed(to, reward);
        return reward;
    }

    /// @notice Calculates the reward amount that will be received for the given stake
    /// @param key The key of the incentive
    /// @param tokenId The ID of the token
    /// @return reward The reward accrued to the NFT for the given incentive thus far
    @External(readonly = true)
    public RewardAmount getRewardInfo (IncentiveKey key, BigInteger tokenId)
    {
        byte[] incentiveId = IncentiveId.compute(key);
        final BigInteger now = now();

        var stakes = stakes(tokenId, incentiveId);
        BigInteger secondsPerLiquidityInsideInitialX128 = stakes.secondsPerLiquidityInsideInitialX128;
        BigInteger liquidity = stakes.liquidity;

        Context.require(liquidity.compareTo(ZERO) > 0, 
            "getRewardInfo: stake does not exist");

        Deposit deposit = deposits.get(tokenId);
        Incentive incentive = incentives.get(incentiveId);

        var snapshot = SnapshotCumulativesInsideResult.fromMap(Context.call(key.pool, "snapshotCumulativesInside", deposit.tickLower, deposit.tickUpper));
        BigInteger secondsPerLiquidityInsideX128 = snapshot.secondsPerLiquidityInsideX128;
        
        return RewardMath.computeRewardAmount (
            incentive.totalRewardUnclaimed,
            incentive.totalSecondsClaimedX128,
            key.startTime,
            key.endTime,
            liquidity,
            secondsPerLiquidityInsideInitialX128,
            secondsPerLiquidityInsideX128,
            now
        );
    }

    /// @dev Stakes a deposited token without doing an ownership check
    private void _stakeToken (IncentiveKey key, BigInteger tokenId) {
        final BigInteger now = now();

        Context.require(now.compareTo(key.startTime) >= 0, 
            "stakeToken: incentive not started");
        Context.require(now.compareTo(key.endTime) < 0, 
            "stakeToken: incentive ended");

        byte[] incentiveId = IncentiveId.compute(key);
        var incentive = incentives.getOrDefault(incentiveId, Incentive.empty());

        Context.require(incentive.totalRewardUnclaimed.compareTo(ZERO) > 0,
             "stakeToken: non-existent incentive"
        );
        Context.require(_stakes.at(tokenId).getOrDefault(incentiveId, Stake.empty()).liquidityNoOverflow.equals(ZERO),
            "stakeToken: token already staked"
        );

        var nftPos = NFTPositionInfo.getPositionInfo(factory, nonfungiblePositionManager, tokenId);
        Address pool = nftPos.pool;
        int tickLower = nftPos.tickLower;
        int tickUpper = nftPos.tickUpper;
        BigInteger liquidity = nftPos.liquidity;

        Context.require(pool.equals(key.pool), 
            "stakeToken: token pool is not the incentive pool");
        Context.require(liquidity.compareTo(ZERO) > 0, 
            "stakeToken: cannot stake token with 0 liquidity");

        var deposit = deposits.get(tokenId);
        deposit.numberOfStakes = deposit.numberOfStakes.add(ONE);
        deposits.set(tokenId, deposit);
        incentive.numberOfStakes = incentive.numberOfStakes.add(ONE);
        incentives.set(incentiveId, incentive);

        var snapshot = SnapshotCumulativesInsideResult.fromMap(Context.call(key.pool, "snapshotCumulativesInside", tickLower, tickUpper));
        BigInteger secondsPerLiquidityInsideX128 = snapshot.secondsPerLiquidityInsideX128;

        if (liquidity.compareTo(MAX_UINT96) >= 0) {
            var stake = new Stake(secondsPerLiquidityInsideX128, MAX_UINT96, liquidity);
            this._stakes.at(tokenId).set(incentiveId, stake);
        } else {
            var stake = _stakes.at(tokenId).getOrDefault(incentiveId, Stake.empty());
            stake.secondsPerLiquidityInsideInitialX128 = secondsPerLiquidityInsideX128;
            stake.liquidityNoOverflow = uint96(liquidity);
            this._stakes.at(tokenId).set(incentiveId, stake);
        }

        this.TokenStaked(tokenId, incentiveId, liquidity);
    }

    @External
    public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) throws Exception {
        Reader reader = new StringReader(new String(_data));
        JsonValue input = Json.parse(reader);
        JsonObject root = input.asObject();
        String method = root.get("method").asString();
        Address token = Context.getCaller();

        switch (method)
        {
            case "createIncentive": {
                JsonObject params = root.get("params").asObject();
                IncentiveKey key = new IncentiveKey(
                    token, // the reward token is the one sent
                    Address.fromString(params.get("pool").asString()),
                    StringUtils.toBigInt(params.get("startTime").asString()),
                    StringUtils.toBigInt(params.get("endTime").asString()),
                    Address.fromString(params.get("refundee").asString())
                  );
                BigInteger reward = _value;
                createIncentive(key, reward);
                break;
            }

            default:
                Context.revert("tokenFallback: Unimplemented tokenFallback action");
        }
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
    public Address nonfungiblePositionManager() {
        return this.nonfungiblePositionManager;
    }

    @External(readonly = true)
    public BigInteger maxIncentiveStartLeadTime() {
        return this.maxIncentiveStartLeadTime;
    }

    @External(readonly = true)
    public BigInteger maxIncentiveDuration() {
        return this.maxIncentiveDuration;
    }

    @External(readonly = true)
    public Incentive incentives (byte[] key) {
        return incentives.get(key);
    }

    @External(readonly = true)
    public Deposit deposits (BigInteger tokenId) {
        return deposits.get(tokenId);
    }

    @External(readonly = true)
    public BigInteger rewards (Address rewardToken, Address owner) {
        return rewards.at(rewardToken).getOrDefault(owner, ZERO);
    }

    /**
     * @notice Returns information about a staked liquidity NFT
     * @param tokenId The ID of the staked token
     * @param incentiveId The ID of the incentive for which the token is staked
     * @return secondsPerLiquidityInsideInitialX128 secondsPerLiquidity represented as a UQ32.128
     * @return liquidity The amount of liquidity in the NFT as of the last time the rewards were computed
     */
    @External(readonly = true)
    public StakesResult stakes(BigInteger tokenId, byte[] incentiveId) {
        Stake stake = this._stakes.at(tokenId).getOrDefault(incentiveId, Stake.empty());
        BigInteger secondsPerLiquidityInsideInitialX128 = stake.secondsPerLiquidityInsideInitialX128;
        BigInteger liquidity = stake.liquidityNoOverflow;
        
        if (liquidity.equals(MAX_UINT96)) {
            liquidity = stake.liquidityIfOverflow;
        }

        return new StakesResult(secondsPerLiquidityInsideInitialX128, liquidity);
    }
}
