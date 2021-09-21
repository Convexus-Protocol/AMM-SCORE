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

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import java.math.BigInteger;

import static org.mockito.Mockito.spy;

public class MultiTokenTest extends TestBase {

    protected static final ServiceManager sm = getServiceManager();
    protected static final Account owner = sm.createAccount();
    protected static final Account alice = sm.createAccount();
    protected static final Account bob = sm.createAccount();
    protected static final Account eve = sm.createAccount();

    protected static Score score;
    protected static IRC31SampleToken spy;

    void tokenSetup() throws Exception {
        score = sm.deploy(owner, IRC31SampleToken.class);
        spy = (IRC31SampleToken) spy(score.getInstance());
        score.setInstance(spy);
    }

    BigInteger mintToken(BigInteger supply) {
        return mintToken(supply, owner);
    }

    BigInteger mintToken(BigInteger supply, Account account) {
        BigInteger newId = getTokenId();
        String uri = "https://craft.network/" + newId;
        score.invoke(account, "mint", newId, supply, uri);
        return newId;
    }

    BigInteger getTokenId() {
        return BigInteger.valueOf((int) (Math.random() * 1000000));
    }
}
