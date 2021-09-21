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
import score.Context;
import score.ObjectReader;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IRC31TransferTest extends MultiTokenTest {

    @BeforeEach
    void setup() throws Exception {
        tokenSetup();
        reset(spy);
    }

    @Test
    void testTransferFromZeroAddress() {
        BigInteger supply = BigInteger.valueOf(100);
        BigInteger newId = mintToken(supply, alice);

        // transfer ownership
        assertThrows(AssertionError.class, () ->
                score.invoke(alice, "transferFrom",
                        alice.getAddress(),
                        IRC31Basic.ZERO_ADDRESS,
                        newId,
                        supply,
                        "test".getBytes()));
    }

    @Test
    void testTransferFromTooMuch() {
        BigInteger supply = BigInteger.valueOf(100);
        BigInteger newId = mintToken(supply);

        // transfer ownership
        assertThrows(AssertionError.class, () ->
                score.invoke(owner, "transferFrom",
                        owner.getAddress(),
                        alice.getAddress(),
                        newId,
                        supply.add(BigInteger.ONE),
                        "test".getBytes()));
    }

    @Test
    void testTransferFrom() {
        BigInteger supply = BigInteger.valueOf(100);
        BigInteger newId = mintToken(supply);
        reset(spy);

        // transfer ownership
        score.invoke(owner, "transferFrom",
                owner.getAddress(),
                alice.getAddress(),
                newId,
                supply,
                "test".getBytes());

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
        assertEquals(from.getValue(), owner.getAddress());
        assertEquals(to.getValue(), alice.getAddress());
        assertEquals(id.getValue(), newId);
        assertEquals(value.getValue(), supply);

        // Balance check
        assertEquals(BigInteger.ZERO, score.call("balanceOf", owner.getAddress(), newId));
        assertEquals(supply, score.call("balanceOf", alice.getAddress(), newId));

        // fail case: alice => bob by owner
        assertThrows(AssertionError.class, () ->
                score.invoke(owner, "transferFrom",
                        alice.getAddress(),
                        bob.getAddress(),
                        newId,
                        supply,
                        "test".getBytes()));

        // approve owner to transfer alice's token
        score.invoke(alice, "setApprovalForAll", owner.getAddress(), true);

        // success case: retry alice => bob by owner
        score.invoke(owner, "transferFrom",
                alice.getAddress(),
                bob.getAddress(),
                newId,
                supply,
                "test".getBytes());

        // Balance check
        assertEquals(BigInteger.ZERO, score.call("balanceOf", owner.getAddress(), newId));
        assertEquals(BigInteger.ZERO, score.call("balanceOf", alice.getAddress(), newId));
        assertEquals(supply, score.call("balanceOf", bob.getAddress(), newId));
    }

    @Test
    void transferFromBatch() {
        BigInteger supply = BigInteger.valueOf(100);

        BigInteger[] ids = new BigInteger[3];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = mintToken(supply);
        }

        BigInteger[] values = {BigInteger.valueOf(50), BigInteger.valueOf(60), BigInteger.valueOf(70)};

        score.invoke(owner, "transferFromBatch", owner.getAddress(), alice.getAddress(), ids, values, "test".getBytes());

        for (int i = 0; i < ids.length; i++) {
            BigInteger balance = (BigInteger) score.call("balanceOf", alice.getAddress(), ids[i]);
            assertEquals(values[i], balance);
        }

        // fail case: alice => bob by owner
        BigInteger[] values2 = {BigInteger.valueOf(10), BigInteger.valueOf(20), BigInteger.valueOf(30)};

        assertThrows(AssertionError.class, () ->
                score.invoke(owner, "transferFromBatch", alice.getAddress(), bob.getAddress(), ids, values2, "test".getBytes()));


        // approve owner to transfer alice's token
        score.invoke(alice, "setApprovalForAll", owner.getAddress(), true);

        // success case: retry alice => bob by owner
        score.invoke(owner, "transferFromBatch", alice.getAddress(), bob.getAddress(), ids, values2, "test".getBytes());

        // check TransferBatch events
        /*
          @EventLog(indexed=3)
          public void TransferBatch(
            Address _operator,
            Address _from,
            Address _to,
            byte[] _ids,
            byte[] _values)
        */
        ArgumentCaptor<Address> operator = ArgumentCaptor.forClass(Address.class);
        ArgumentCaptor<Address> from = ArgumentCaptor.forClass(Address.class);
        ArgumentCaptor<Address> to = ArgumentCaptor.forClass(Address.class);
        ArgumentCaptor<byte[]> idsCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<byte[]> valuesCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(spy, times(2)).TransferBatch(
                operator.capture(),
                from.capture(),
                to.capture(),
                idsCaptor.capture(),
                valuesCaptor.capture());

        assertEquals(operator.getAllValues().get(0), owner.getAddress());
        assertEquals(from.getAllValues().get(0), owner.getAddress());
        assertEquals(to.getAllValues().get(0), alice.getAddress());

        assertEquals(operator.getAllValues().get(1), owner.getAddress());
        assertEquals(from.getAllValues().get(1), alice.getAddress());
        assertEquals(to.getAllValues().get(1), bob.getAddress());
        byte[] data0 = idsCaptor.getAllValues().get(1);
        byte[] data1 = valuesCaptor.getAllValues().get(1);

        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", data0);
        BigInteger[] idsRlp = new BigInteger[ids.length];
        reader.beginList();
        for (int i = 0; i < idsRlp.length; i++) {
            idsRlp[i] = reader.readBigInteger();
        }
        reader.end();

        ObjectReader reader2 = Context.newByteArrayObjectReader("RLPn", data1);
        BigInteger[] valuesRlp = new BigInteger[ids.length];
        reader2.beginList();
        for (int i = 0; i < valuesRlp.length; i++) {
            valuesRlp[i] = reader2.readBigInteger();
        }
        reader2.end();

        for (int i = 0; i < ids.length; i++) {
            assertEquals(idsRlp[i], ids[i]);
        }

        for (int i = 0; i < values.length; i++) {
            assertEquals(valuesRlp[i], values2[i]);
        }

        // balanceBatch check
        BigInteger[] exp = {BigInteger.valueOf(50), BigInteger.valueOf(40), BigInteger.valueOf(30)};
        Address[] owners = {owner.getAddress(), alice.getAddress(), bob.getAddress()};
        BigInteger[] balances = (BigInteger[]) score.call("balanceOfBatch", owners, ids);
        for (int i = 0; i < balances.length; i++) {
            assertEquals(exp[i], balances[i]);
        }
    }

    @Test
    void testTransferFromBatchZeroAddress() {
        BigInteger supply = BigInteger.valueOf(100);

        BigInteger[] ids = new BigInteger[3];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = mintToken(supply);
        }

        BigInteger[] values = {BigInteger.valueOf(50), BigInteger.valueOf(60), BigInteger.valueOf(70)};

        assertThrows(AssertionError.class, () ->
                score.invoke(owner, "transferFromBatch", owner.getAddress(), IRC31Basic.ZERO_ADDRESS, ids, values, "test".getBytes()));
    }

    @Test
    void testTransferFromBatchIdValueMismatch() {
        BigInteger supply = BigInteger.valueOf(100);

        BigInteger[] ids = new BigInteger[3];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = mintToken(supply);
        }

        BigInteger[] values = {BigInteger.valueOf(50)};

        assertThrows(AssertionError.class, () ->
                score.invoke(owner, "transferFromBatch", owner.getAddress(), alice.getAddress(), ids, values, "test".getBytes()));
    }

    @Test
    void testTransferFromBatchTooMuch() {
        BigInteger supply = BigInteger.valueOf(100);

        BigInteger[] ids = new BigInteger[3];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = mintToken(supply);
        }

        BigInteger[] values = {BigInteger.valueOf(50), supply.add(BigInteger.ONE), BigInteger.valueOf(70)};

        // transfer ownership
        assertThrows(AssertionError.class, () ->
                score.invoke(owner, "transferFromBatch", owner.getAddress(), alice.getAddress(), ids, values, "test".getBytes()));
    }
}
