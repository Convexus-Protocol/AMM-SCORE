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

package com.iconloop.score.token.irc31;

import score.Address;
import score.BranchDB;
import score.ByteArrayObjectWriter;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

public abstract class IRC31Basic implements IRC31 {

    // ================================================
    // Consts
    // ================================================
    public static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);

    // ================================================
    // SCORE DB
    // ================================================
    // id => (owner => balance)
    private final BranchDB<BigInteger, DictDB<Address, BigInteger>> balances = Context.newBranchDB("balances", BigInteger.class);
    // owner => (operator => approved)
    private final BranchDB<Address, DictDB<Address, Boolean>> operatorApproval = Context.newBranchDB("approval", Boolean.class);
    // id => token URI
    private final DictDB<BigInteger, String> tokenURIs = Context.newDictDB("token_uri", String.class);

    // ================================================
    // External methods
    // ================================================

    @External(readonly=true)
    public BigInteger balanceOf(Address _owner, BigInteger _id) {
        return balances.at(_id).getOrDefault(_owner, BigInteger.ZERO);
    }

    @External(readonly=true)
    public BigInteger[] balanceOfBatch(Address[] _owners, BigInteger[] _ids) {
        Context.require(_owners.length == _ids.length,
                "_owners array size must match with _ids array size");

        BigInteger[] balances = new BigInteger[_owners.length];
        for (int i = 0; i < _owners.length; i++) {
            balances[i] = balanceOf(_owners[i], _ids[i]);
        }
        return balances;
    }

    @External(readonly=true)
    public String tokenURI(BigInteger _id) {
        return tokenURIs.get(_id);
    }

    @External
    public void transferFrom(Address _from, Address _to, BigInteger _id, BigInteger _value, @Optional byte[] _data) {
        final Address caller = Context.getCaller();

        Context.require(!_to.equals(ZERO_ADDRESS),
                "_to must be non-zero");
        Context.require(_from.equals(caller) || this.isApprovedForAll(_from, caller),
                "Need operator approval for 3rd party transfers");
        Context.require(BigInteger.ZERO.compareTo(_value) <= 0 && _value.compareTo(balanceOf(_from, _id)) <= 0,
                "Insufficient funds");

        // Transfer funds
        DictDB<Address, BigInteger> balance = balances.at(_id);
        balance.set(_from, balanceOf(_from, _id).subtract(_value));
        balance.set(_to, balanceOf(_to, _id).add(_value));

        // Emit event
        this.TransferSingle(caller, _from, _to, _id, _value);

        if (_to.isContract()) {
            // Call {@code onIRC31Received} if the recipient is a contract
            Context.call(_to, "onIRC31Received", caller, _from, _id, _value, _data == null ? new byte[]{} : _data);
        }
    }

    @External
    public void transferFromBatch(Address _from, Address _to, BigInteger[] _ids, BigInteger[] _values, @Optional byte[] _data) {
        final Address caller = Context.getCaller();

        Context.require(!_to.equals(ZERO_ADDRESS),
                "_to must be non-zero");
        Context.require(_ids.length == _values.length,
                "id/value pairs mismatch");
        Context.require(_from.equals(caller) || this.isApprovedForAll(_from, caller),
                "Need operator approval for 3rd party transfers");

        for (int i = 0; i < _ids.length; i++) {
            BigInteger _id = _ids[i];
            BigInteger _value = _values[i];

            Context.require(_value.compareTo(BigInteger.ZERO) >= 0,
                    "Insufficient funds");

            BigInteger balanceFrom = balanceOf(_from, _id);

            Context.require(_value.compareTo(balanceFrom) <= 0,
                    "Insufficient funds");

            // Transfer funds
            BigInteger balanceTo = balanceOf(_to, _id);
            DictDB<Address, BigInteger> balance = balances.at(_id);
            balance.set(_from, balanceFrom.subtract(_value));
            balance.set(_to, balanceTo.add(_value));
        }

        // Emit event
        this.TransferBatch(caller, _from, _to, rlpEncode(_ids), rlpEncode(_values));

        if (_to.isContract()) {
            // call {@code onIRC31BatchReceived} if the recipient is a contract
            Context.call(_to, "onIRC31BatchReceived",
                    caller, _from, _ids, _values, _data == null ? new byte[]{} : _data);
        }
    }

    @External
    public void setApprovalForAll(Address _operator, boolean _approved) {
        final Address caller = Context.getCaller();

        operatorApproval.at(caller).set(_operator, _approved);
        this.ApprovalForAll(caller, _operator, _approved);
    }

    @External(readonly=true)
    public boolean isApprovedForAll(Address _owner, Address _operator) {
        return operatorApproval.at(_owner).getOrDefault(_operator, false);
    }

    // ================================================
    // Event Logs
    // ================================================

    @EventLog(indexed=3)
    public void TransferSingle(Address _operator, Address _from, Address _to, BigInteger _id, BigInteger _value) {}

    @EventLog(indexed=3)
    public void TransferBatch(Address _operator, Address _from, Address _to, byte[] _ids, byte[] _values) {}

    @EventLog(indexed=2)
    public void ApprovalForAll(Address _owner, Address _operator, boolean _approved) {}

    @EventLog(indexed=1)
    public void URI(BigInteger _id, String _value) {}

    // ================================================
    // Internal methods
    // ================================================

    /**
     * Convert a list of BigInteger to a RLP-encoded byte array
     *
     * @param ids A list of BigInteger
     * @return a RLP encoded byte array
     */
    protected static byte[] rlpEncode(BigInteger[] ids) {
        Context.require(ids != null);

        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");

        writer.beginList(ids.length);
        for (BigInteger v : ids) {
            writer.write(v);
        }
        writer.end();

        return writer.toByteArray();
    }

    protected void _setTokenURI(BigInteger _id, String _uri) {
        Context.require(_uri.length() > 0, "Uri should be set");
        tokenURIs.set(_id, _uri);
        this.URI(_id, _uri);
    }

    private void _mintInternal(Address owner, BigInteger id, BigInteger amount) {
        Context.require(amount.compareTo(BigInteger.ZERO) > 0, "Invalid amount");

        BigInteger balance = balanceOf(owner, id);
        balances.at(id).set(owner, balance.add(amount));
    }

    protected void _mint(Address owner, BigInteger id, BigInteger amount) {
        _mintInternal(owner, id, amount);

        // emit transfer event for Mint semantic
        TransferSingle(owner, ZERO_ADDRESS, owner, id, amount);
    }

    protected void _mintBatch(Address owner, BigInteger[] ids, BigInteger[] amounts) {
        Context.require(ids.length == amounts.length, "id/amount pairs mismatch");

        for (int i = 0; i < ids.length; i++) {
            BigInteger id = ids[i];
            BigInteger amount = amounts[i];
            _mintInternal(owner, id, amount);
        }

        // emit transfer event for Mint semantic
        TransferBatch(owner, ZERO_ADDRESS, owner, rlpEncode(ids), rlpEncode(amounts));
    }

    private void _burnInternal(Address owner, BigInteger id, BigInteger amount) {
        Context.require(amount.compareTo(BigInteger.ZERO) > 0, "Invalid amount");

        BigInteger balance = balanceOf(owner, id);
        Context.require(balance.compareTo(amount) >= 0, "Insufficient funds");
        balances.at(id).set(owner, balance.subtract(amount));
    }

    protected void _burn(Address owner, BigInteger id, BigInteger amount) {
        _burnInternal(owner, id, amount);

        // emit transfer event for Burn semantic
        TransferSingle(owner, owner, ZERO_ADDRESS, id, amount);
    }

    protected void _burnBatch(Address owner, BigInteger[] ids, BigInteger[] amounts) {
        Context.require(ids.length == amounts.length, "id/amount pairs mismatch");

        for (int i = 0; i < ids.length; i++) {
            BigInteger id = ids[i];
            BigInteger amount = amounts[i];
            _burnInternal(owner, id, amount);
        }

        // emit transfer event for Burn semantic
        TransferBatch(owner, owner, ZERO_ADDRESS, rlpEncode(ids), rlpEncode(amounts));
    }
}
