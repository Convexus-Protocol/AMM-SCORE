/*
 * Copyright 2019 ICON Foundation
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

package foundation.icon.test.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.IconAmount;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static foundation.icon.test.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SwitchyFactoryScore extends Score {

    public SwitchyFactoryScore(Score other) {
        super(other);
    }

    public static SwitchyFactoryScore mustDeploy(TransactionHandler txHandler, Wallet wallet)
            throws IOException, TransactionFailureException, ResultTimeoutException {
        LOG.infoEntering("deploy", "SwitchyFactory");
        RpcObject params = new RpcObject.Builder()
                .build();
        Score score = txHandler.deploy(wallet, getFilePath("Switchy-Factory"), params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        return new SwitchyFactoryScore(score);
    }
}
