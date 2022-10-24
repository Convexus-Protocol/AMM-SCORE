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

package exchange.convexus.integration.factory;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.test.Env;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import exchange.convexus.integration.score.ConvexusFactoryScore;
import exchange.convexus.integration.score.IRC2BasicTokenScore;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;

import static foundation.icon.test.Env.LOG;

public class ConvexusFactoryTest extends TestBase {
    private static TransactionHandler txHandler;
    private final static KeyWallet operator = KeyWallet.load(new Bytes("573b555367d6734ea0fecd0653ba02659fa19f7dc6ee5b93ec781350bda27376"));
    private static KeyWallet[] wallets = new KeyWallet[100];
    
    final int TICK_SPACINGS[] = {10, 60, 200};
    final int FEE_AMOUNTS[] = {500, 3000, 10000};
    final int LOW = 0;
    final int MEDIUM = 1;
    final int HIGH = 2;

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = new Env.Chain(7, operator, "https://berlin.net.solidwallet.io");
        IconService iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);
    }

    void icxFunding (BigInteger amount) throws Exception {
        amount = ICX.multiply(amount);

        for (int i = 1; i < wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
        }

        for (int i = 1; i < wallets.length; i++) {
            ensureIcxBalance(txHandler, wallets[i].getAddress(), BigInteger.ZERO, amount);
        }
    }

    void icxRefund () throws Exception {
        for (KeyWallet wallet : wallets) {
            txHandler.refundAll(wallet);
        }
    }

    @AfterAll
    static void shutdown() throws Exception {
    }

    @Test
    public void deployAndStartTest() throws Exception {
        ConvexusFactoryScore factory = ConvexusFactoryScore.mustDeploy(txHandler, operator);
        startTest(factory);
    }

    private void startTest(ConvexusFactoryScore factory) throws Exception {
        LOG.infoEntering("setup", "initial wallets");
        LOG.info("Address of operator: " + operator.getAddress());

        LOG.info("Deploying IUSDC token");
        var usdc = IRC2BasicTokenScore.install(txHandler, operator, "ICON USDC", "IUSDC", BigInteger.valueOf(18), BigInteger.valueOf(1_000_000_000L));

        LOG.info("Deploying SICX token");
        var sicx = IRC2BasicTokenScore.install(txHandler, operator, "Staked ICX", "sICX", BigInteger.valueOf(18), BigInteger.valueOf(1_000_000_000L));

        LOG.info("Setting the pool contract bytes to Factory");
        byte[] fileContent = Files.readAllBytes(new File(Score.getFilePath("Convexus-Core:Contracts:Pool")).toPath());
        factory.setPoolContract(operator, fileContent);

        LOG.info("Creating a new SICX / USDC pool using the factory");
        Address poolAddress = factory.createPool(operator, 
            sicx.getAddress(), 
            usdc.getAddress(), 
            FEE_AMOUNTS[MEDIUM]
        );
        LOG.info("Newly created pool : " + poolAddress);

        LOG.infoExiting();
    }
}
