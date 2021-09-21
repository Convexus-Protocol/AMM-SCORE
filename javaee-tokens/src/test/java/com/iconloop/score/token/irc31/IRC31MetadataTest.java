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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import score.Address;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class IRC31MetadataTest extends MultiTokenTest {

    @BeforeEach
    void setup() throws Exception {
        tokenSetup();
        reset(spy);
    }

    @Test
    void testTransferSingle() {
        BigInteger supply = BigInteger.valueOf(100);
        BigInteger newId = mintToken(supply);

        /*
            @EventLog(indexed=3)
            public void TransferSingle(
                Address _operator,
                Address _from,
                Address _to,
                Integer _id,
                BigInteger _value) {}
        */
        ArgumentCaptor<Address> operator = ArgumentCaptor.forClass(Address.class);
        ArgumentCaptor<Address> from = ArgumentCaptor.forClass(Address.class);
        ArgumentCaptor<Address> to = ArgumentCaptor.forClass(Address.class);
        ArgumentCaptor<BigInteger> id = ArgumentCaptor.forClass(BigInteger.class);
        ArgumentCaptor<BigInteger> value = ArgumentCaptor.forClass(BigInteger.class);

        verify(spy).TransferSingle(
                operator.capture(),
                from.capture(),
                to.capture(),
                id.capture(),
                value.capture());

        // Check TransferSingle event
        assertEquals(operator.getValue(), owner.getAddress());
        assertEquals(from.getValue(), IRC31Basic.ZERO_ADDRESS);
        assertEquals(to.getValue(), owner.getAddress());
        assertEquals(id.getValue(), newId);
        assertEquals(value.getValue(), supply);
    }

    @Test
    void testTokenURI() {
        BigInteger supply = BigInteger.valueOf(100);
        BigInteger newId = mintToken(supply);
        String uri = (String) score.call("tokenURI", newId);
        String expectedUri = "https://craft.network/" + newId;
        assertEquals(uri, expectedUri);
    }

    @Test
    void testSetTokenURI() {
        BigInteger supply = BigInteger.valueOf(100);
        BigInteger newId = mintToken(supply);
        reset(spy);

        String newURI = ((String) score.call("tokenURI", newId)) + "_updated";
        score.invoke(owner, "setTokenURI", newId, newURI);

        // Test event
        /*
            @EventLog(indexed=1)
            public void URI(
                Integer _id,
                String _value) {}
        */
        ArgumentCaptor<BigInteger> id = ArgumentCaptor.forClass(BigInteger.class);
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(spy).URI(id.capture(), value.capture());

        assertEquals(id.getValue(), newId);
        assertEquals(value.getValue(), newURI);

        // Check updated URI
        assertEquals(score.call("tokenURI", newId), newURI);
    }

    @Test
    void testSetTokenURIOnlyOwner() {
        BigInteger supply = BigInteger.valueOf(100);
        BigInteger newId = mintToken(supply);
        reset(spy);

        String newURI = ((String) score.call("tokenURI", newId)) + "_updated";
        assertThrows(AssertionError.class, () ->
                score.invoke(eve, "setTokenURI", newId, newURI));
    }
}
