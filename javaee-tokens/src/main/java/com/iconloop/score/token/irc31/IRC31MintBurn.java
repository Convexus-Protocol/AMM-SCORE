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
import score.Context;
import score.DictDB;
import score.annotation.External;

import java.math.BigInteger;

public abstract class IRC31MintBurn extends IRC31Basic {

    // ================================================
    // SCORE DB 
    // ================================================
    // id ==> creator
    private final DictDB<BigInteger, Address> creators = Context.newDictDB("creators", Address.class);

    // ================================================
    // External methods
    // ================================================

    /**
     * Creates a new token type and assigns _supply to creator
     *
     * @param _id     ID of the token
     * @param _supply The initial token supply
     * @param _uri    The token URI
     */
    @External
    public void mint(BigInteger _id, BigInteger _supply, String _uri) {
        Context.require(creators.get(_id) == null, "Token is already minted");
        Context.require(_supply.compareTo(BigInteger.ZERO) > 0, "Supply should be positive");

        final Address caller = Context.getCaller();
        creators.set(_id, caller);

        // mint tokens
        super._mint(caller, _id, _supply);
        // set token URI
        super._setTokenURI(_id, _uri);
    }

    /**
     * Destroys tokens for a given amount
     *
     * @param _id     ID of the token
     * @param _amount The amount of tokens to burn
     */
    @External
    public void burn(BigInteger _id, BigInteger _amount) {
        Context.require(creators.get(_id) != null, "Invalid token id");
        Context.require(_amount.compareTo(BigInteger.ZERO) > 0, "Amount should be positive");

        // burn tokens
        super._burn(Context.getCaller(), _id, _amount);
    }

    /**
     * Updates the given token URI
     *
     * @param _id  ID of the token
     * @param _uri The token URI
     */
    @External
    public void setTokenURI(BigInteger _id, String _uri) {
        Context.require(Context.getCaller().equals(creators.get(_id)), "Not token creator");

        super._setTokenURI(_id, _uri);
    }
}
