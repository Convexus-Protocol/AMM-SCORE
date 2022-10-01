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

package exchange.convexus.periphery.governor;

import java.math.BigInteger;
import exchange.convexus.cxs.ICXS;
import exchange.convexus.governor.MethodCall;
import exchange.convexus.periphery.timelock.Timelock;
import exchange.convexus.timelock.ITimelock;
import exchange.convexus.utils.MathUtils;
import exchange.convexus.utils.TimeUtils;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

/**
 * @title Convexus Governor contract
 */
public class ConvexusGovernor {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    private static final String NAME = "ConvexusGovernor";

    // Contract name
    private final String name;

    /// The address of the Convexus Protocol Timelock
    private final Address timelock;

    /// The address of the Convexus governance token
    private final Address cxs;

    private static final long VOTING_PERIOD = 3600 / 2 * 24 * 3; 
    private static final long VOTING_DELAY = 1; 
    private static final long PROPOSAL_MAX_OPERATIONS = 100; 

    // ================================================
    // DB Variables
    // ================================================
    /// The total number of proposals
    private final VarDB<BigInteger> proposalCount = Context.newVarDB(NAME + "_proposalCount", BigInteger.class);

    /// The official record of all proposals ever proposed
    private final DictDB<BigInteger, Proposal> proposals = Context.newDictDB(NAME + "_proposals", Proposal.class);

    /// The latest proposal for each proposer
    private final DictDB<Address, BigInteger> latestProposalIds = Context.newDictDB(NAME + "_latestProposalIds", BigInteger.class);

    // ================================================
    // Event Logs
    // ================================================
    /// An event emitted when a new proposal is created
    @EventLog(indexed = 1)
    protected void ProposalCreated (
        BigInteger id,
        String description
    ) {}

    /// An event emitted when a vote has been cast on a proposal
    @EventLog
    protected void VoteCast (
        Address voter, 
        BigInteger proposalId,
        boolean support,
        BigInteger votes
    ) {}

    /// An event emitted when a proposal has been canceled
    @EventLog
    protected void ProposalCanceled (
        BigInteger id
    ) {}

    /// An event emitted when a proposal has been queued in the Timelock
    @EventLog
    protected void ProposalQueued (
        BigInteger id,
        BigInteger eta
    ) {}

    /// An event emitted when a proposal has been executed in the Timelock
    @EventLog(indexed = 1)
    protected void ProposalExecuted (
        BigInteger id
    ) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public ConvexusGovernor (
        Address timelock,
        Address cxs
    ) {
        this.name = "Convexus Governor";
        this.timelock = timelock;
        this.cxs = cxs;
    }

    @External
    public BigInteger propose (MethodCall[] calls, String description) {
        final Address caller = Context.getCaller();

        Context.require(ICXS.getPriorVotes(this.cxs, caller, Context.getBlockHeight() - 1).compareTo(proposalThreshold()) > 0,
            NAME + "::propose: proposer votes below proposal threshold");
        
        Context.require(calls.length != 0,
            NAME + "::propose: must provide actions");
    
        Context.require(calls.length <= PROPOSAL_MAX_OPERATIONS,
            NAME + "::propose: too many actions");
        
        BigInteger latestProposalId = this.latestProposalIds.get(caller);
        if (latestProposalId != null) {
            ProposalState proposersLatestProposalState = this.state(latestProposalId);
            Context.require(proposersLatestProposalState != ProposalState.Active, 
                NAME + "::propose: one live proposal per proposer, found an already active proposal");
            Context.require(proposersLatestProposalState != ProposalState.Pending, 
                NAME + "::propose: one live proposal per proposer, found an already active proposal");
        }

        long startBlock = Context.getBlockHeight() + VOTING_DELAY;
        long endBlock = startBlock + VOTING_PERIOD;

        // proposalCount++
        BigInteger proposalCount = this.proposalCount.get();
        proposalCount = proposalCount.add(BigInteger.ONE);
        this.proposalCount.set(proposalCount);

        Proposal newProposal = new Proposal(
            proposalCount, 
            caller, 
            BigInteger.ZERO,
            calls, 
            startBlock, 
            endBlock, 
            BigInteger.ZERO, 
            BigInteger.ZERO, 
            false, 
            false
        );

        this.proposals.set(newProposal.id, newProposal);
        this.latestProposalIds.set(newProposal.proposer, newProposal.id);

        this.ProposalCreated(newProposal.id, description);
        return newProposal.id;
    }

    @External
    public void queue (BigInteger proposalId) {
        Context.require(state(proposalId) == ProposalState.Succeeded,
            NAME + "::queue: proposal can only be queued if it is succeeded");
        
        Proposal proposal = this.proposals.get(proposalId);
        BigInteger eta = TimeUtils.now().add(ITimelock.delay(timelock));
        for (var call : proposal.calls) {
            _queueOrRevert(call, eta);
        }
        proposal.eta = eta;
        this.proposals.set(proposalId, proposal);
        this.ProposalQueued(proposalId, eta);
    }

    private void _queueOrRevert(MethodCall call, BigInteger eta) {
        byte[] hash = Timelock.getHash(call, eta);
        Context.require(ITimelock.queuedTransactions(this.timelock, hash),
            NAME + "::_queueOrRevert: proposal action already queued at eta");
        
        ITimelock.queueTransaction(this.timelock, call, eta);
    }

    @External
    public void execute (BigInteger proposalId) {
        Context.require(state(proposalId) == ProposalState.Queued, 
            NAME + "::execute: proposal can only be executed if it is queued");
        
        Proposal proposal = this.proposals.get(proposalId);
        proposal.executed = true;
        this.proposals.set(proposalId, proposal);

        for (var call : proposal.calls) {
            ITimelock.executeTransaction(this.timelock, call, proposal.eta);
        }

        this.ProposalExecuted(proposalId);
    }

    @External
    public void cancel (BigInteger proposalId) {
        Context.require(this.state(proposalId) == ProposalState.Executed,
           NAME + "::cancel: cannot cancel executed proposal");
        
        Proposal proposal = this.proposals.get(proposalId);
        Context.require(ICXS.getPriorVotes(this.cxs, proposal.proposer, Context.getBlockHeight() - 1).compareTo(proposalThreshold()) < 0,
            NAME + "::cancel: proposer above threshold");
        
        proposal.canceled = true;
        this.proposals.set(proposalId, proposal);

        for (var call : proposal.calls) {
            ITimelock.cancelTransaction(this.timelock, call, proposal.eta);
        }

        this.ProposalCanceled(proposalId);
    }

    @External(readonly = true)
    public MethodCall[] getActions (BigInteger proposalId) {
        return this.proposals.get(proposalId).calls;
    }

    @External(readonly = true)
    public Receipt getReceipt (BigInteger proposalsId, Address voter) {
        return this.proposals.get(proposalsId).receipts(voter).get();
    }

    private ProposalState state(BigInteger proposalId) {
        Context.require(proposalCount.get().compareTo(proposalId) >= 0 
                     && proposalId.compareTo(BigInteger.ZERO) > 0,
            NAME + "::state: invalid proposal id");
        
        long blockNumber = Context.getBlockHeight();

        Proposal proposal = this.proposals.get(proposalId);
        if (proposal.canceled) {
            return ProposalState.Canceled;
        } else if (blockNumber <= proposal.startBlock) {
            return ProposalState.Pending;
        } else if (blockNumber <= proposal.endBlock) {
            return ProposalState.Active;
        } else if (proposal.forVotes.compareTo(proposal.againstVotes) <= 0 
                || proposal.forVotes.compareTo(quorumVotes()) < 0) {
            return ProposalState.Defeated;
        } else if (proposal.eta.equals(BigInteger.ZERO)) {
            return ProposalState.Succeeded;
        } else if (proposal.executed) {
            return ProposalState.Executed;
        } else if (TimeUtils.now().compareTo(proposal.eta.add(ITimelock.GRACE_PERIOD(this.timelock))) >= 0) {
            return ProposalState.Expired;
        } else {
            return ProposalState.Queued;
        }
    }

    @External
    public void castVote (BigInteger proposalId, boolean support) {
        _castVote (Context.getCaller(), proposalId, support);
    }

    private void _castVote(Address voter, BigInteger proposalId, boolean support) {
        Context.require(state(proposalId) == ProposalState.Active, 
            NAME + "::_castVote: voting is closed");

        Proposal proposal = this.proposals.get(proposalId);
        Receipt receipt = proposal.receipts(voter).get();

        Context.require(receipt.hasVoted == false,
            NAME + "::_castVote: voter already voted");
        
        BigInteger votes = ICXS.getPriorVotes(this.cxs, voter, proposal.startBlock);

        if (support) {
            proposal.forVotes = proposal.forVotes.add(votes);
        } else {
            proposal.againstVotes = proposal.againstVotes.add(votes);
        }

        this.proposals.set(proposalId, proposal);

        receipt.hasVoted = true;
        receipt.support = support;
        receipt.votes = votes;
        proposal.receipts(voter).set(receipt);

        this.VoteCast(voter, proposalId, support, votes);
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }

    /// The number of votes in support of a proposal required in order for a quorum to be reached and for a vote to succeed
    @External(readonly = true)
    public BigInteger quorumVotes() {
        return BigInteger.valueOf(400_000).multiply(MathUtils.pow10(18));  // 400,000 = 4% of CXS
    }

    /// The number of votes required in order for a voter to become a proposer
    @External(readonly = true)
    public BigInteger proposalThreshold() {
        return BigInteger.valueOf(100_000).multiply(MathUtils.pow10(18)); // 100,000 = 1% of CXS
    }

    /// The maximum number of actions that can be included in a proposal
    @External(readonly = true)
    public BigInteger proposalMaxOperations() {
        return BigInteger.valueOf(PROPOSAL_MAX_OPERATIONS); // 100 actions
    }

    /// The delay before voting on a proposal may take place, once proposed
    @External(readonly = true)
    public BigInteger votingDelay() {
        return BigInteger.valueOf(VOTING_DELAY); // 1 block
    }

    /// The duration of voting on a proposal, in blocks
    @External(readonly = true)
    public BigInteger votingPeriod() {
        return BigInteger.valueOf(VOTING_PERIOD);  // ~3 days in blocks (assuming 2s blocks)
    }
}
