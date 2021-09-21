/*
 * Copyright 2021 ICONLOOP Inc.
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
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public class IRC31SampleReceiver implements IRC31Receiver {
    // allowlist of token contracts
    final private DictDB<Address, Boolean> originators = Context.newDictDB("originators", Boolean.class);

    @External
    public void setOriginator(Address _origin, boolean _approved) {
        Context.require(Context.getOwner().equals(Context.getCaller()), "Not contract owner");
        Context.require(_origin.isContract(), "Not contract address");
        originators.set(_origin, _approved);
    }

    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {
        final Address caller = Context.getCaller();
        Context.require(originators.get(caller) != null, "Unrecognized token contract");
        IRC31Received(caller, _operator, _from, _id, _value, _data);
    }

    @External
    public void onIRC31BatchReceived(Address _operator, Address _from, BigInteger[] _ids, BigInteger[] _values, byte[] _data) {
        final Address caller = Context.getCaller();
        Context.require(originators.get(caller) != null, "Unrecognized token contract");
        for (int i = 0; i < _ids.length; i++) {
            BigInteger _id = _ids[i];
            BigInteger _value = _values[i];
            IRC31Received(caller, _operator, _from, _id, _value, _data);
        }
    }

    @EventLog(indexed=3)
    public void IRC31Received(Address _origin, Address _operator, Address _from,
                              BigInteger _id, BigInteger _value, byte[] _data) {
    }
}
