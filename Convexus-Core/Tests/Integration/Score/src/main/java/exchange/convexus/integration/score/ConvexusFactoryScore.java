/*
 * Copyright 2022 Convexus Protocol
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

package exchange.convexus.integration.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;

import static foundation.icon.test.Env.LOG;

public class ConvexusFactoryScore extends Score {

    public ConvexusFactoryScore(Score other) {
        super(other);
    }

    public static ConvexusFactoryScore mustDeploy(TransactionHandler txHandler, Wallet wallet)
            throws IOException, TransactionFailureException, ResultTimeoutException {
        
        LOG.infoEntering("deploy", "ConvexusFactory");
        RpcObject params = new RpcObject.Builder()
                .build();
        Score score = txHandler.deploy(wallet, getFilePath("Convexus-Core:Contracts:Factory"), params);
        LOG.info("ConvexusFactory deployed at: " + score.getAddress());
        LOG.infoExiting();
        return new ConvexusFactoryScore(score);
    }

    public Address createPool (
        Wallet fromWallet, 
        Address tokenA, 
        Address tokenB, 
        int fee
    ) throws IOException, ResultTimeoutException {
        RpcObject params = new RpcObject.Builder()
                .put("tokenA", new RpcValue(tokenA))
                .put("tokenB", new RpcValue(tokenB))
                .put("fee", new RpcValue(BigInteger.valueOf(fee)))
                .build();
        TransactionResult result = invokeAndWaitResult(fromWallet, "createPool", params);
        return ensurePoolCreated(result, tokenA, tokenB, fee);
    }

    public TransactionResult setPoolContract (Wallet fromWallet, byte[] contractBytes)
            throws IOException, ResultTimeoutException {
        RpcObject params = new RpcObject.Builder()
                .put("contractBytes", new RpcValue(contractBytes))
                .build();
        TransactionResult result = invokeAndWaitResult(fromWallet, "setPoolContract", params);
        return result;
    }

    private Address ensurePoolCreated(TransactionResult result, Address tokenA, Address tokenB, int fee) throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "PoolCreated(Address,Address,int,int,Address)");
        if (event != null) {
            Address token0 = event.getIndexed().get(1).asAddress();
            Address token1 = event.getIndexed().get(2).asAddress();
            BigInteger _fee = event.getIndexed().get(3).asInteger();
            // BigInteger tickSpacing = event.getData().get(0).asInteger();
            Address pool = event.getData().get(1).asAddress();
            if ((tokenA.equals(token0) && tokenB.equals(token1))
            ||  (tokenA.equals(token1) && tokenB.equals(token0))
            && fee == _fee.intValue()
            ) {
                return pool;
            }
        }
        throw new IOException("Failed to get Confirmation.");
    }
}
