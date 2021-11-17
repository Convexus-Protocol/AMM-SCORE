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

package foundation.icon.test.cases;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
// import foundation.icon.icx.data.Address;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.test.Env;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.IRC2BasicToken;
import foundation.icon.test.score.Score;
import foundation.icon.test.score.ConvexusFactoryScore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import exchange.convexus.utils.MathUtils;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;

import static foundation.icon.test.Env.LOG;

public class ConvexusFactoryIntTest extends TestBase {
    private static TransactionHandler txHandler;
    private static KeyWallet[] wallets;

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        IconService iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        wallets = new KeyWallet[5];
        BigInteger amount = ICX.multiply(BigInteger.valueOf(50));
        for (int i = 0; i < wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
        }
        for (KeyWallet wallet : wallets) {
            ensureIcxBalance(txHandler, wallet.getAddress(), BigInteger.ZERO, amount);
        }
    }

    @AfterAll
    static void shutdown() throws Exception {
        for (KeyWallet wallet : wallets) {
            txHandler.refundAll(wallet);
        }
    }

    @Test
    public void deployAndStartTest() throws Exception {
        // deploy MultiSigWallet SCORE
        ConvexusFactoryScore score = ConvexusFactoryScore.mustDeploy(txHandler, wallets[0]);
        startTest(score);
    }

    private void startTest(ConvexusFactoryScore score) throws Exception {
        LOG.infoEntering("setup", "initial wallets");
        KeyWallet ownerWallet = wallets[0];
        KeyWallet aliceWallet = wallets[1];
        KeyWallet bobWallet = wallets[2];
        LOG.info("Address of owner: " + ownerWallet.getAddress());
        LOG.info("Address of Alice: " + aliceWallet.getAddress());
        LOG.info("Address of Bob:   " + bobWallet.getAddress());
        // Address scoreAddress = score.getAddress();

        LOG.info("Deploying IUSDC token");
        var usdc = IRC2BasicToken.install(txHandler, ownerWallet, "ICON USDC", "IUSDC", BigInteger.valueOf(8), MathUtils.pow10(18));

        LOG.info("Deploying SICX token");
        var sicx = IRC2BasicToken.install(txHandler, ownerWallet, "Staked ICX", "sICX", BigInteger.valueOf(8), MathUtils.pow10(18));

        // LOG.info("Deploying bnUSD token");
        // var bnusd = IRC2BasicToken.install(txHandler, ownerWallet, "Balanced USD", "bnUSD", BigInteger.valueOf(8), MathUtils.pow10(18));

        LOG.info("Setting the pool contract bytes to Factory");
        byte[] fileContent = Files.readAllBytes(new File(Score.getFilePath("Convexus-Pool")).toPath());
        score.setPoolContract(ownerWallet, fileContent);

        LOG.info("Deploying a new SICX / USDC pool");
        score.createPool(ownerWallet, sicx.getAddress(), usdc.getAddress(), 500);

        LOG.infoExiting();
    }
}
