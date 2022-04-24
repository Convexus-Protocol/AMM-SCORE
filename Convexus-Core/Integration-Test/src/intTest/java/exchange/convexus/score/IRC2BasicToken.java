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

package exchange.convexus.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;

public class IRC2BasicToken extends Score {
    public IRC2BasicToken(Score other) {
        super(other);
    }

    public static IRC2BasicToken install(
        TransactionHandler txHandler, 
        Wallet wallet,
        String name,
        String symbol,
        BigInteger decimals,
        BigInteger initialSupply
    ) throws TransactionFailureException, ResultTimeoutException, IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_name", new RpcValue(name))
                .put("_symbol", new RpcValue(symbol))
                .put("_decimals", new RpcValue(decimals))
                .put("_initialSupply", new RpcValue(initialSupply))
                .build();
        return install(txHandler, wallet, params);
    }

    public static IRC2BasicToken install(TransactionHandler txHandler, Wallet wallet, RpcObject params)
            throws TransactionFailureException, ResultTimeoutException, IOException {
        return new IRC2BasicToken(txHandler.deploy(wallet, getFilePath("Convexus-Commons:Tokens:Contracts:irc2"), params));
    }
}