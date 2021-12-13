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

package exchange.convexus.score;

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

public class ConvexusPoolScore extends Score {

    public ConvexusPoolScore(Score other) {
        super(other);
    }

    public static ConvexusPoolScore mustDeploy(
      TransactionHandler txHandler, 
      Wallet wallet,
      // Patch this after SCORE can deploy other SCOREs
      Address _token0,
      Address _token1,
      Address _factory,
      int fee,
      int tickSpacing
    ) throws IOException, TransactionFailureException, ResultTimeoutException {
        LOG.infoEntering("deploy", "ConvexusPool");
        RpcObject params = new RpcObject.Builder()
                .put("_token0", new RpcValue(_token0))
                .put("_token1", new RpcValue(_token1))
                .put("_factory", new RpcValue(_factory))
                .put("fee", new RpcValue(BigInteger.valueOf(fee)))
                .put("tickSpacing", new RpcValue(BigInteger.valueOf(tickSpacing)))
                .build();
        Score score = txHandler.deploy(wallet, getFilePath("Convexus-Core:Contracts:Pool"), params);
        LOG.info("ConvexusPool deployed at: " + score.getAddress());
        LOG.infoExiting();
        return new ConvexusPoolScore(score);
    }

    public TransactionResult initialize(Wallet fromWallet, BigInteger sqrtPriceX96)
            throws IOException, ResultTimeoutException {
        RpcObject params = new RpcObject.Builder()
                .put("sqrtPriceX96", new RpcValue(sqrtPriceX96))
                .build();
        TransactionResult result = invokeAndWaitResult(fromWallet, "initialize", params);
        return result;
    }
}
