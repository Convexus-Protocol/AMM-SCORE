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

package exchange.convexus.periphery.cxs;

import static exchange.convexus.utils.AddressUtils.ZERO_ADDRESS;
import static exchange.convexus.utils.TimeUtils.now;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import exchange.convexus.utils.MathUtils;
import exchange.convexus.utils.TimeUtils;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class CXS {
    // ================================================
    // Consts
    // ================================================
    // Contract name
    private final String NAME = "CXS";

    // IRC2 token name for this token
    private final String name = "Convexus Token";

    // IRC2 token symbol for this token
    private final String symbol = "CXS";

    // IRC2 token decimals for this token
    private final int decimals = 18;

    // Total number of tokens in circulation : 1 billion CXS
    private final BigInteger INITIAL_TOTAL_SUPPLY = BigInteger.valueOf(1_000_000_000).multiply(MathUtils.pow10(18));;
    private final VarDB<BigInteger> totalSupply = Context.newVarDB(NAME + "_totalSupply", BigInteger.class);

    // Minimum time between mints
    private final BigInteger minimumTimeBetweenMints = TimeUtils.ONE_YEAR;

    // Cap on the percentage of totalSupply that can be minted at each mint
    private final BigInteger mintCap = TWO;
 
    // ================================================
    // SCORE DB
    // ================================================
    // Address which may mint new tokens
    private final VarDB<Address> minter = Context.newVarDB(NAME + "_minter", Address.class);

    // The timestamp after which minting may occur
    private final VarDB<BigInteger> mintingAllowedAfter = Context.newVarDB(NAME + "_mintingAllowedAfter", BigInteger.class);

    // Official record of token balances for each account
    private final DictDB<Address, BigInteger> balances = Context.newDictDB(NAME + "_balances", BigInteger.class);

    // A record of each accounts delegate
    private final DictDB<Address, Address> delegates = Context.newDictDB(NAME + "_delegates", Address.class);

    // A record of votes checkpoints for each account, by index
    private final BranchDB<Address, DictDB<Integer, Checkpoint>> checkpoints = Context.newBranchDB(NAME + "_checkpoints", Checkpoint.class);

    // The number of checkpoints for each account
    private final DictDB<Address, Integer> numCheckpoints = Context.newDictDB(NAME + "_numCheckpoints", Integer.class);

    // ================================================
    // Event Logs
    // ================================================
    // An event thats emitted when the minter address is changed
    @EventLog
    protected void MinterChanged(Address minter, Address newMinter) {}

    // An event thats emitted when an account changes its delegate
    @EventLog(indexed = 3)
    protected void DelegateChanged(Address delegator, Address fromDelegate, Address toDelegate) {}

    // An event thats emitted when a delegate account's vote balance changes
    @EventLog(indexed = 1)
    protected void DelegateVotesChanged(Address delegate, BigInteger previousBalance, BigInteger newBalance) {}

    // The standard EIP-20 transfer event
    @EventLog(indexed = 3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {}

    // The standard EIP-20 approval event
    @EventLog(indexed = 2)
    protected void Approval(Address owner, Address spender, BigInteger amount) {}

    // ================================================
    // Methods
    // ================================================
    /**
     * @notice Construct a new CXS token
     * @param account The initial account to grant all the tokens
     * @param minter The account with minting ability
     * @param mintingAllowedAfter The timestamp after which minting may occur
     */
    public CXS (
        Address account, 
        Address minter, 
        BigInteger mintingAllowedAfter
    ) {
        // Initial deploy
        if (balances.get(account) == null) {
            Context.require(mintingAllowedAfter.compareTo(now()) >= 0,
            "CXS: minting can only begin after deployment");

            balances.set(account, INITIAL_TOTAL_SUPPLY);
            this.Transfer(ZERO_ADDRESS, account, INITIAL_TOTAL_SUPPLY, "genesis".getBytes());
            
            this.minter.set(minter);
            this.MinterChanged(ZERO_ADDRESS, minter);

            this.mintingAllowedAfter.set(mintingAllowedAfter);
        }
    }

    /**
     * @notice Change the minter address
     * @param minter The address of the new minter
     */
    @External
    public void setMinter (Address minter) {
        Address oldMinter = this.minter.get();
        Context.require(Context.getCaller().equals(oldMinter), 
            "setMinter: Only the minter can change the minter address");

        this.MinterChanged(oldMinter, minter);
        this.minter.set(minter);
    }

    /**
     * @notice Mint new tokens
     * @param dst The address of the destination account
     * @param rawAmount The number of tokens to be minted
     */
    @External
    public void mint (Address dst, BigInteger amount) {
        final BigInteger now = now();
        final BigInteger ONE_HUNDRED = BigInteger.valueOf(100);

        Context.require(Context.getCaller().equals(this.minter.get()), 
            "mint: Only the minter can change the minter address");

        Context.require(now.compareTo(this.mintingAllowedAfter.get()) >= 0, 
            "mint: minting not allowed yet");

        Context.require(!dst.equals(ZERO_ADDRESS), 
            "mint: cannot transfer to the zero address");
        
        // record the mint
        this.mintingAllowedAfter.set(now.add(this.minimumTimeBetweenMints));

        // mint the amount
        BigInteger oldSupply = this.totalSupply.get();
        Context.require(amount.compareTo(oldSupply.multiply(this.mintCap).divide(ONE_HUNDRED)) <= 0,
            "mint: exceeded mint cap");
        
        this.totalSupply.set(oldSupply.add(amount));

        // transfer the amount to the recipient
        safeSetBalance(dst, this.balances.get(dst).add(amount));
        this.Transfer(ZERO_ADDRESS, dst, amount, "mint".getBytes());

        // move delegates
        _moveDelegates(ZERO_ADDRESS, this.delegates.get(dst), amount);
    }

    /**
     * @notice Get the number of tokens held by the `account`
     * @param account The address of the account to get the balance of
     * @return The number of tokens held
     */
    @External(readonly = true)
    public BigInteger balanceOf(Address account) {
        return safeGetBalance(account);
    }

    /**
     * @notice Transfer `amount` tokens from `Context.getCaller()` to `dst`
     * @param dst The address of the destination account
     * @param rawAmount The number of tokens to transfer
     * @return Whether or not the transfer succeeded
     */
    @External
    public void transfer (Address _to, BigInteger _value, @Optional byte[] _data) {
        _transferTokens(Context.getCaller(), _to, _value, _data);
    }

    /**
     * @notice Delegate votes from `Context.getCaller()` to `delegatee`
     * @param delegatee The address to delegate votes to
     */
    @External
    public void delegate(Address delegatee) {
        _delegate(Context.getCaller(), delegatee);
    }

    /**
     * @notice Gets the current votes balance for `account`
     * @param account The address to get votes balance
     * @return The number of current votes for `account`
     */
    @External(readonly = true)
    public BigInteger getCurrentVotes (Address account) {
        int nCheckpoints = this.numCheckpoints.getOrDefault(account, 0);
        return nCheckpoints > 0 
            ? this.checkpoints.at(account).get(nCheckpoints).votes 
            : ZERO;
    }
    
    /**
     * @notice Determine the prior number of votes for an account as of a block number
     * @dev Block number must be a finalized block or else this function will revert to prevent misinformation.
     * @param account The address of the account to check
     * @param blockNumber The block number to get the vote balance at
     * @return The number of votes the account had as of the given block
     */
    @External(readonly = true)
    public BigInteger getPriorVotes(Address account, long blockNumber) {
        Context.require(blockNumber < Context.getBlockHeight(), 
            "getPriorVotes: not yet determined");

        int nCheckpoints = this.numCheckpoints.getOrDefault(account, 0);
        if (nCheckpoints == 0) {
            return ZERO;
        }

        // First check most recent balance
        if (this.checkpoints.at(account).get(nCheckpoints - 1).fromBlock <= blockNumber) {
            return this.checkpoints.at(account).get(nCheckpoints - 1).votes;
        }

        // Next check implicit zero balance
        if (this.checkpoints.at(account).get(0).fromBlock > blockNumber) {
            return ZERO;
        }

        int lower = 0;
        int upper = nCheckpoints - 1;

        while (upper > lower) {
            int center = upper - (upper - lower) / 2; // ceil, avoiding overflow
            Checkpoint cp = this.checkpoints.at(account).get(center);
            if (cp.fromBlock == blockNumber) {
                return cp.votes;
            } else if (cp.fromBlock < blockNumber) {
                lower = center;
            } else {
                upper = center - 1;
            }
        }

        return this.checkpoints.at(account).get(lower).votes;
    }

    // ================================================
    // Private methods
    // ================================================
    private void _delegate(Address delegator, Address delegatee) {
        Address currentDelegate = this.delegates.get(delegator);
        BigInteger delegatorBalance = this.balances.get(delegator);
        this.delegates.set(delegator, delegatee);

        this.DelegateChanged(delegator, currentDelegate, delegatee);

        _moveDelegates(currentDelegate, delegatee, delegatorBalance);
    }

    private void _transferTokens(Address src, Address dst, BigInteger amount, @Optional byte[] _data) {
        Context.require(src != ZERO_ADDRESS, 
            "_transferTokens: cannot transfer from the zero address");
        Context.require(dst != ZERO_ADDRESS, 
            "_transferTokens: cannot transfer to the zero address");

        // check some basic requirements
        Context.require(amount.compareTo(ZERO) >= 0, 
            "_transferTokens: _value needs to be positive");
        Context.require(safeGetBalance(src).compareTo(amount) >= 0, 
            "_transferTokens: Insufficient balance");

        // adjust the balances
        safeSetBalance(src, safeGetBalance(src).subtract(amount));
        safeSetBalance(dst, safeGetBalance(dst).subtract(amount));

        _moveDelegates(this.delegates.get(src), this.delegates.get(dst), amount);
        
        // if the recipient is SCORE, call 'tokenFallback' to handle further operation
        byte[] dataBytes = (_data == null) ? new byte[0] : _data;
        if (dst.isContract()) {
            Context.call(dst, "tokenFallback", src, amount, dataBytes);
        }
        
        this.Transfer(src, dst, amount, _data);
    }

    private void safeSetBalance(Address owner, BigInteger amount) {
        this.balances.set(owner, amount);
    }

    private BigInteger safeGetBalance(Address owner) {
        return balances.getOrDefault(owner, ZERO);
    }
    
    private void _moveDelegates(Address srcRep, Address dstRep, BigInteger amount) {
        if (!srcRep.equals(dstRep) && amount.compareTo(ZERO) > 0) {
            if (!srcRep.equals(ZERO_ADDRESS)) {
                int srcRepNum = this.numCheckpoints.get(srcRep);
                BigInteger srcRepOld = srcRepNum > 0 ? this.checkpoints.at(srcRep).get(srcRepNum - 1).votes : ZERO;
                Context.require(srcRepOld.compareTo(amount) <= 0, 
                    "_moveDelegates: vote amount underflows");
                BigInteger srcRepNew = srcRepOld.subtract(amount);
                _writeCheckpoint(srcRep, srcRepNum, srcRepOld, srcRepNew);
            }
            if (!dstRep.equals(ZERO_ADDRESS)) {
                int dstRepNum = this.numCheckpoints.get(dstRep);
                BigInteger dstRepOld = dstRepNum > 0 ? this.checkpoints.at(dstRep).get(dstRepNum - 1).votes : ZERO;
                BigInteger dstRepNew = dstRepOld.add(amount);
                _writeCheckpoint(dstRep, dstRepNum, dstRepOld, dstRepNew);
            }
        }
    }

    private void _writeCheckpoint(Address delegatee, int nCheckpoints, BigInteger oldVotes, BigInteger newVotes) {
        long blockNumber = Context.getBlockHeight();
        var checkpoint = this.checkpoints.at(delegatee).get(nCheckpoints - 1);
  
        if (nCheckpoints > 0 
         && this.checkpoints.at(delegatee).get(nCheckpoints - 1).fromBlock == blockNumber
        ) {
            checkpoint.votes = newVotes;
            this.checkpoints.at(delegatee).set(nCheckpoints - 1, checkpoint);
        } else {
            this.checkpoints.at(delegatee).set(nCheckpoints, new Checkpoint(blockNumber, newVotes));
            this.numCheckpoints.set(delegatee, nCheckpoints + 1);
        }
  
        this.DelegateVotesChanged(delegatee, oldVotes, newVotes);
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }

    @External(readonly = true)
    public BigInteger decimals() {
        return BigInteger.valueOf(this.decimals);
    }

    @External(readonly = true)
    public String symbol() {
        return this.symbol;
    }

    @External(readonly = true)
    public Address minter() {
        return this.minter.get();
    }
    
    @External(readonly=true)
    public BigInteger totalSupply() {
        return this.totalSupply.getOrDefault(ZERO);
    }
    
    @External(readonly=true)
    public BigInteger mintingAllowedAfter() {
        return this.mintingAllowedAfter.getOrDefault(ZERO);
    }
    
    @External(readonly=true)
    public BigInteger minimumTimeBetweenMints() {
        return this.minimumTimeBetweenMints;
    }
    
    @External(readonly=true)
    public BigInteger mintCap () {
        return this.mintCap;
    }

    @External(readonly = true)
    public Address delegates (Address account) {
        return this.delegates(account);
    }

    @External(readonly = true)
    public Checkpoint checkpoints (Address account, int nCheckpoints) {
        return this.checkpoints.at(account).get(nCheckpoints);
    }
}
