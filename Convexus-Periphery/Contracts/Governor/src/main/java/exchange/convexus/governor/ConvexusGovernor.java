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

package exchange.convexus.governor;

import java.math.BigInteger;

import exchange.convexus.utils.MathUtils;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
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
    private Address timelock;

    /// The address of the Convexus governance token
    private Address cxs;

    // ================================================
    // DB Variables
    // ================================================
    /// The total number of proposals
    private final VarDB<BigInteger> proposalCount = Context.newVarDB(NAME + "_proposalCount", BigInteger.class);

    /// The address of the Governor Guardian
    private final VarDB<Address> guardian = Context.newVarDB(NAME + "_guardian", Address.class);

    private final DictDB<BigInteger, Proposal> proposals = Context.newDictDB(NAME + "_proposals", Proposal.class);

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public ConvexusGovernor (
    ) {
        this.name = "Convexus Governor";
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
    public long proposalMaxOperations() {
        return 10; // 10 actions
    }

    /// The delay before voting on a proposal may take place, once proposed
    @External(readonly = true)
    public long votingDelay() {
        return 1; // 1 block
    }

    /// The duration of voting on a proposal, in blocks
    @External(readonly = true)
    public long votingPeriod() {
        return 3600 / 2 * 24 * 3;  // ~3 days in blocks (assuming 2s blocks)
    }
}
