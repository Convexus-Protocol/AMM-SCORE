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

package exchange.convexus.testtokens;

import score.Address;
import score.Context;
import score.annotation.External;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.Optional;
import java.math.BigInteger;

import static exchange.convexus.utils.AddressUtils.ZERO_ADDRESS;

public class Bnusd {
    private final String name;
    private final String symbol;
    private final int decimals;
    private final VarDB<BigInteger> totalSupply = Context.newVarDB("totalSupply", BigInteger.class);
    private final DictDB<Address, BigInteger> balances = Context.newDictDB("balances", BigInteger.class);

    public Bnusd(String _name, String _symbol, int _decimals) {
        this.name = _name;
        this.symbol = _symbol;
        this.decimals = _decimals;

        // decimals must be larger than 0 and less than 21
        Context.require(this.decimals >= 0, "this.decimals >= 0");
        Context.require(this.decimals <= 21, "this.decimals <= 21");
    }

    @External(readonly=true)
    public String name() {
        return name;
    }

    @External(readonly=true)
    public String symbol() {
        return symbol;
    }

    @External(readonly=true)
    public int decimals() {
        return decimals;
    }

    @External(readonly=true)
    public BigInteger totalSupply() {
        return totalSupply.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly=true)
    public BigInteger balanceOf(Address _owner) {
        return safeGetBalance(_owner);
    }

    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        Address _from = Context.getCaller();

        // check some basic requirements
        Context.require(_value.compareTo(BigInteger.ZERO) >= 0, "Value must be >= 0");
        Context.require(safeGetBalance(_from).compareTo(_value) >= 0, String.format("%s: Not enough balance (%s < %s)", _from, safeGetBalance(_from), _value));

        // adjust the balances
        safeSetBalance(_from, safeGetBalance(_from).subtract(_value));
        safeSetBalance(_to, safeGetBalance(_to).add(_value));

        // if the recipient is SCORE, call 'tokenFallback' to handle further operation
        byte[] dataBytes = (_data == null) ? new byte[0] : _data;
        if (_to.isContract()) {
            Context.call(_to, "tokenFallback", _from, _value, dataBytes);
        }

        // emit Transfer event
        Transfer(_from, _to, _value, dataBytes);
    }

    /**
     * Creates `amount` tokens and assigns them to `owner`, increasing the total supply.
     */
    protected void _mint(Address owner, BigInteger amount) {
        Context.require(!ZERO_ADDRESS.equals(owner), "!ZERO_ADDRESS.equals(owner)");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, "amount.compareTo(BigInteger.ZERO) >= 0");

        totalSupply.set(totalSupply.getOrDefault(BigInteger.ZERO).add(amount));
        safeSetBalance(owner, safeGetBalance(owner).add(amount));
        Transfer(ZERO_ADDRESS, owner, amount, "mint".getBytes());
    }

    /**
     * Destroys `amount` tokens from `owner`, reducing the total supply.
     */
    protected void _burn(Address owner, BigInteger amount) {
        Context.require(!ZERO_ADDRESS.equals(owner), "!ZERO_ADDRESS.equals(owner)");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, "amount.compareTo(BigInteger.ZERO) >= 0");
        Context.require(safeGetBalance(owner).compareTo(amount) >= 0, "safeGetBalance(owner).compareTo(amount) >= 0");

        safeSetBalance(owner, safeGetBalance(owner).subtract(amount));
        totalSupply.set(totalSupply.getOrDefault(BigInteger.ZERO).subtract(amount));
        Transfer(owner, ZERO_ADDRESS, amount, "burn".getBytes());
    }

    private BigInteger safeGetBalance(Address owner) {
        return balances.getOrDefault(owner, BigInteger.ZERO);
    }

    private void safeSetBalance(Address owner, BigInteger amount) {
        balances.set(owner, amount);
    }

    @EventLog(indexed=3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {}

    @External
    public void mint(BigInteger amount) {
        // simple access control - only the contract owner can mint new token
        Context.require(Context.getCaller().equals(Context.getOwner()), "Only owner can call this method");
        _mint(Context.getCaller(), amount);
    }

    @External
    public void mintTo(Address account, BigInteger amount) {
        // simple access control - only the contract owner can mint new token
        Context.require(Context.getCaller().equals(Context.getOwner()), "Only owner can call this method");
        _mint(account, amount);
    }
}