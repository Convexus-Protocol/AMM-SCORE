/*
 * Copyright 2022 ICONation
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

package exchange.convexus.treasuryvester;

import java.math.BigInteger;

import exchange.convexus.interfaces.irc2.IIRC2;
import exchange.convexus.utils.JSONUtils;
import exchange.convexus.utils.TimeUtils;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

public class TreasuryVester {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    private static final String NAME = "TreasuryVester";

    // Contract name
    private final String name;

    private final Address cxs;

    private final BigInteger vestingAmount;
    private final BigInteger vestingBegin;
    private final BigInteger vestingCliff;
    private final BigInteger vestingEnd;

    // ================================================
    // DB Variables
    // ================================================
    private final VarDB<BigInteger> lastUpdate = Context.newVarDB(NAME + "_lastUpdate", BigInteger.class);
    private final VarDB<Address> recipient = Context.newVarDB(NAME + "_recipent", Address.class);

    // ================================================
    // Event Logs
    // ================================================

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public TreasuryVester (
        Address cxs,
        Address recipient,
        BigInteger vestingAmount,
        BigInteger vestingBegin,
        BigInteger vestingCliff,
        BigInteger vestingEnd
    ) {
        Context.require(vestingBegin.compareTo(TimeUtils.now()) >= 0,
            "TreasuryVester::constructor: vesting begin too early");
        Context.require(vestingCliff.compareTo(vestingBegin) >= 0,
            "TreasuryVester::constructor: cliff is too early");
        Context.require(vestingEnd.compareTo(vestingCliff) > 0,
            "TreasuryVester::constructor: end is too early");

        this.name = "Convexus Treasury Vester";

        this.cxs = cxs;

        this.vestingAmount = vestingAmount;
        this.vestingBegin = vestingBegin;
        this.vestingCliff = vestingCliff;
        this.vestingEnd = vestingEnd;

        if (this.recipient.get() == null) {
            this.recipient.set(recipient);
        }
        if (this.lastUpdate.get() == null) {
            this.lastUpdate.set(vestingBegin);
        }
    }

    @External
    public void setRecipient (Address recipient) {
        Context.require(Context.getCaller().equals(this.recipient.get()),
            "TreasuryVester::setRecipient: unauthorized");
        
        this.recipient.set(recipient);
    }

    @External
    public void claim () {
        final BigInteger now = TimeUtils.now();

        Context.require(now.compareTo(vestingCliff) >= 0,
            "TreasuryVester::claim: not time yet");
        
        BigInteger amount;
        if (now.compareTo(vestingEnd) >= 0) {
            amount = IIRC2.balanceOf(this.cxs, Context.getAddress());
        } else {
            amount = vestingAmount.multiply(now.subtract(lastUpdate.get())).divide(vestingEnd.subtract(vestingBegin));
            this.lastUpdate.set(now);
        }

        IIRC2.transfer(this.cxs, this.recipient.get(), amount, JSONUtils.method("pay"));
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
