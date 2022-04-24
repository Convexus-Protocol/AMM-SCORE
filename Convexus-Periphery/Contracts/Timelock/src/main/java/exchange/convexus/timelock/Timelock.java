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

package exchange.convexus.timelock;

import java.math.BigInteger;

import exchange.convexus.structs.MethodCall;
import exchange.convexus.utils.BytesUtils;
import exchange.convexus.utils.TimeUtils;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

public class Timelock {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    private static final String NAME = "Timelock";

    // Contract name
    private final String name;

    // 14 days
    private final BigInteger GRACE_PERIOD = TimeUtils.ONE_DAY.multiply(BigInteger.valueOf(14));
    // 2 days
    private final BigInteger MINIMUM_DELAY = TimeUtils.ONE_DAY.multiply(BigInteger.valueOf(2));
    // 30 days
    private final BigInteger MAXIMUM_DELAY = TimeUtils.ONE_DAY.multiply(BigInteger.valueOf(30));

    // ================================================
    // DB Variables
    // ================================================
    private final VarDB<Address> admin = Context.newVarDB(NAME + "_admin", Address.class);
    private final VarDB<Address> pendingAdmin = Context.newVarDB(NAME + "_pendingAdmin", Address.class);
    private final VarDB<BigInteger> delay = Context.newVarDB(NAME + "_delay", BigInteger.class);

    private final DictDB<byte[], Boolean> queuedTransactions = Context.newDictDB(NAME + "_queuedTransactions", Boolean.class);

    // ================================================
    // Event Logs
    // ================================================
    @EventLog
    protected void NewDelay(BigInteger delay) {}

    @EventLog
    protected void NewAdmin(Address admin) {}

    @EventLog
    protected void NewPendingAdmin(Address pendingAdmin) {}

    @EventLog
    protected void QueueTransaction(byte[] hash, BigInteger eta) {}

    @EventLog
    protected void CancelTransaction(byte[] hash, BigInteger eta) {}

    @EventLog
    protected void ExecuteTransaction(byte[] hash, BigInteger eta) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public Timelock (
        Address admin,
        BigInteger delay
    ) {
        this.name = "Convexus Timelock";
        
        Context.require(delay.compareTo(MINIMUM_DELAY) >= 0, 
            "Timelock::constructor: Delay must exceed minimum delay.");
        Context.require(delay.compareTo(MAXIMUM_DELAY) <= 0, 
            "Timelock::setDelay: Delay must not exceed maximum delay.");

        if (this.admin.get() == null) {
            this.admin.set(admin);
        }

        if (this.delay.get() == null) {
            this.delay.set(delay);
        }
    }

    public static byte[] getHash (MethodCall call, BigInteger eta) {
        return Context.hash("sha3-256", BytesUtils.concat(
            call.toBytes(),
            eta.toByteArray()
        ));
    }

    @Payable
    @External
    public void fallback () {}

    @External
    public void setDelay (BigInteger delay) {
        Context.require(Context.getCaller().equals(Context.getAddress()), 
            "Timelock::setDelay: Call must come from Timelock.");
        Context.require(delay.compareTo(MINIMUM_DELAY) >= 0, 
            "Timelock::setDelay: Delay must exceed minimum delay.");
        Context.require(delay.compareTo(MAXIMUM_DELAY) <= 0, 
            "Timelock::setDelay: Delay must not exceed maximum delay.");

        this.delay.set(delay);
        this.NewDelay(delay);
    }

    @External
    public void acceptAdmin () {
        final Address caller = Context.getCaller();

        Context.require(caller.equals(this.pendingAdmin.get()),
            "Timelock::acceptAdmin: Call must come from pendingAdmin");

        this.admin.set(caller);
        this.pendingAdmin.set(null);

        this.NewAdmin(caller);
    }

    @External
    public void setPendingAdmin (Address pendingAdmin) {
        Context.require(Context.getCaller().equals(Context.getAddress()),
            "Timelock::setPendingAdmin: Call must come from Timelock");
        
        this.pendingAdmin.set(pendingAdmin);
        this.NewPendingAdmin(pendingAdmin);
    }

    @External
    public byte[] queueTransaction (MethodCall call, BigInteger eta) {
        // Access control
        Context.require(Context.getCaller().equals(this.admin.get()),
            "Timelock::queueTransaction: Call must come from admin");

        Context.require(eta.compareTo(TimeUtils.now().add(delay.get())) >= 0,
            "Timelock::queueTransaction: Estimated execution block must satisfy delay");

        // OK
        byte[] hash = getHash(call, eta);

        this.queuedTransactions.set(hash, true);

        this.QueueTransaction(hash, eta);
        return hash;
    }

    @External
    public void cancelTransaction (MethodCall call, BigInteger eta) {
        // Access control
        Context.require(Context.getCaller().equals(this.admin.get()),
            "Timelock::queueTransaction: Call must come from admin");

        // OK
        byte[] hash = getHash(call, eta);
        this.queuedTransactions.set(hash, false);

        this.CancelTransaction(hash, eta);
    }

    @External
    public Object executeTransaction (MethodCall call, BigInteger eta) {
        // Access control
        Context.require(Context.getCaller().equals(this.admin.get()),
            "Timelock::queueTransaction: Call must come from admin");

        byte[] hash = getHash(call, eta);
        
        Context.require(queuedTransactions.get(hash) == true, 
            "Timelock::executeTransaction: Transaction hasn't been queued");

        final BigInteger now = TimeUtils.now();
        Context.require(now.compareTo(eta) >= 0,
            "Timelock::executeTransaction: Transaction hasn't surpassed time lock");

        Context.require(now.compareTo(eta.add(GRACE_PERIOD)) <= 0,
            "Timelock::executeTransaction: Transaction is stale");

        // OK
        queuedTransactions.set(hash, false);

        Object result = call.call();

        this.ExecuteTransaction(hash, eta);

        return result;
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
