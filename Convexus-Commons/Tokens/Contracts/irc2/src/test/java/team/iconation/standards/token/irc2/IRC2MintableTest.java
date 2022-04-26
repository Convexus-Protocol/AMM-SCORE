/*
 * Copyright 2022 ICONLOOP Inc.
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

package team.iconation.standards.token.irc2;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import score.Address;

import java.math.BigInteger;

import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class IRC2MintableTest extends TestBase {
    private final String name = "MyIRC2Mintable";
    private final String symbol = "MIM";
    private final int decimals = 18;
    private final BigInteger initialSupply = BigInteger.valueOf(1000);

    private BigInteger totalSupply = initialSupply.multiply(TEN.pow(decimals));
    private final ServiceManager sm = getServiceManager();
    private final Account owner = sm.createAccount();
    private final Account alice = sm.createAccount();
    private final Account eve = sm.createAccount();
    private Score tokenScore;
    private IRC2Mintable tokenSpy;

    @BeforeEach
    public void setup() throws Exception {
        tokenScore = sm.deploy(owner, IRC2Mintable.class, name, symbol, decimals, initialSupply);
        owner.addBalance(symbol, totalSupply);

        // setup spy object against the tokenScore object
        tokenSpy = (IRC2Mintable) spy(tokenScore.getInstance());
        tokenScore.setInstance(tokenSpy);
    }

    @Test
    void mint() {
        final Address zeroAddress = new Address(new byte[Address.LENGTH]);
        assertEquals(totalSupply, tokenScore.call("totalSupply"));

        // mint 10 token to self
        BigInteger amount = TEN.pow(decimals);
        tokenScore.invoke(owner, "mint", amount);
        owner.addBalance(symbol, amount);
        totalSupply = totalSupply.add(amount);
        assertEquals(owner.getBalance(symbol), tokenScore.call("balanceOf", owner.getAddress()));
        assertEquals(totalSupply, tokenScore.call("totalSupply"));
        verify(tokenSpy).Transfer(zeroAddress, owner.getAddress(), amount, "mint".getBytes());
    }

    @Test
    void mintToAlice() {
        final Address zeroAddress = new Address(new byte[Address.LENGTH]);
        assertEquals(totalSupply, tokenScore.call("totalSupply"));

        // mint 10 token to alice
        BigInteger amount = TEN.pow(decimals);
        tokenScore.invoke(owner, "mintTo", alice.getAddress(), amount);
        alice.addBalance(symbol, amount);
        totalSupply = totalSupply.add(amount);
        assertEquals(alice.getBalance(symbol), tokenScore.call("balanceOf", alice.getAddress()));
        assertEquals(totalSupply, tokenScore.call("totalSupply"));
        verify(tokenSpy).Transfer(zeroAddress, alice.getAddress(), amount, "mint".getBytes());
    }

    @Test
    void mintEve() {
        // mint 10 token but fail, eve is not owner
        BigInteger amount = TEN.pow(decimals);
        assertThrows(AssertionError.class, () -> tokenScore.invoke(eve, "mint", amount));
    }

    @Test
    void mintToEve() {
        // mint 10 token to Alice but fail, eve is not owner
        BigInteger amount = TEN.pow(decimals);
        assertThrows(AssertionError.class, () -> tokenScore.invoke(eve, "mintTo", alice.getAddress(), amount));
    }

    @Test
    void setMinter() {
        // Change minter role to Alice
        tokenScore.invoke(owner, "setMinter", alice.getAddress());

        // owner shouldn't be able to mint anymore
        BigInteger amount = TEN.pow(decimals);
        assertThrows(AssertionError.class, () -> tokenScore.invoke(owner, "mint", amount));
        assertThrows(AssertionError.class, () -> tokenScore.invoke(eve, "mint", amount));

        // Change the minter role back to owner
        tokenScore.invoke(owner, "setMinter", owner.getAddress());
    }
}
