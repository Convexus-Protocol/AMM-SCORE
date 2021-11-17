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

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Map;

import score.Address;
import score.Context;
import score.DictDB;
import score.ObjectReader;
import score.ObjectWriter;
import score.VarDB;

public class Proposal {
    private static final String NAME = "ConvexusProposal";

    /// @notice Unique id for looking up a proposal
    BigInteger id;

    /// @notice Creator of the proposal
    Address proposer;

    /// @notice The timestamp that the proposal will be available for execution, set once the vote succeeds
    BigInteger eta;

    // Methods to be called
    MethodCall[] calls;
    
    long startBlock;

    /// @notice The block at which voting ends: votes must be cast prior to this block
    long endBlock;

    /// @notice Current number of votes in favor of this proposal
    BigInteger forVotes;

    /// @notice Current number of votes in opposition to this proposal
    BigInteger againstVotes;

    /// @notice Flag marking whether the proposal has been canceled
    boolean canceled;

    /// @notice Flag marking whether the proposal has been executed
    boolean executed;

    /// @notice Receipts of ballots for the entire set of voters
    private final VarDB<Receipt> receipts (Address voter) {
        return Context.newVarDB(NAME + "_receipts_" + voter.toString(), Receipt.class);
    }

    public Proposal (
        BigInteger id,
        Address proposer,
        BigInteger eta,
        MethodCall[] calls,
        long startBlock,
        long endBlock,
        BigInteger forVotes,
        BigInteger againstVotes,
        boolean canceled,
        boolean executed
    ) {
        this.id = id;
        this.proposer = proposer;
        this.eta = eta;
        this.calls = calls;
        this.startBlock = startBlock;
        this.endBlock = endBlock;
        this.forVotes = forVotes;
        this.againstVotes = againstVotes;
        this.canceled = canceled;
        this.executed = executed;
    }

    public static Proposal readObject (ObjectReader r) {

        BigInteger id = r.readBigInteger();
        Address proposer = r.readAddress();
        BigInteger eta = r.readBigInteger();
        MethodCall[] calls = new MethodCall[r.readInt()];

        r.beginList();
        for (int i = 0; i < calls.length; i++) {
            calls[i] = r.read(MethodCall.class);
        }
        r.end();

        long startBlock = r.readLong();
        long endBlock = r.readLong();
        BigInteger forVotes = r.readBigInteger();
        BigInteger againstVotes = r.readBigInteger();
        boolean canceled = r.readBoolean();
        boolean executed = r.readBoolean();

        return new Proposal(
            id,
            proposer,
            eta,
            calls,
            startBlock,
            endBlock,
            forVotes,
            againstVotes,
            canceled,
            executed
        );
    }

    public static void writeObject(ObjectWriter w, Proposal obj) {
        w.write(obj.id);
        w.write(obj.proposer);
        w.write(obj.eta);
        w.write(obj.calls.length);
        w.beginList(obj.calls.length);
        for (int i = 0; i < obj.calls.length; i++) {
            w.write(obj.calls[i]);
        }
        w.end();
        w.write(obj.startBlock);
        w.write(obj.endBlock);
        w.write(obj.forVotes);
        w.write(obj.againstVotes);
        w.write(obj.canceled);
        w.write(obj.executed);
    }
}