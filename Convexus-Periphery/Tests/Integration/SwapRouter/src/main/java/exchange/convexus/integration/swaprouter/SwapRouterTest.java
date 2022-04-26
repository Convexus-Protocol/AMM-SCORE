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

package exchange.convexus.integration.swaprouter;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.test.Env;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import exchange.convexus.integration.score.ConvexusFactoryScore;
import exchange.convexus.integration.score.ConvexusPoolScore;
import exchange.convexus.integration.score.IRC2BasicTokenScore;
import exchange.convexus.utils.MathUtils;

import static java.math.BigInteger.ONE;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.file.Files;
import static foundation.icon.test.Env.LOG;

public class SwapRouterTest extends TestBase {
    private static TransactionHandler txHandler;
    private static KeyWallet[] wallets;

    final int TICK_SPACINGS[] = {10, 60, 200};
    final int FEE_AMOUNTS[] = {500, 3000, 10000};
    final int LOW = 0;
    final int MEDIUM = 1;
    final int HIGH = 2;

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
        startTest();
    }

    private void startTest() throws Exception {
        LOG.infoEntering("setup", "initial wallets");
        KeyWallet ownerWallet = wallets[0];
        LOG.info("Address of owner: " + ownerWallet.getAddress());

        LOG.info("Deploying IUSDC token");
        var usdc = IRC2BasicTokenScore.install(txHandler, ownerWallet, "ICON USDC", "IUSDC", BigInteger.valueOf(8), MathUtils.pow10(18));

        LOG.info("Deploying SICX token");
        var sicx = IRC2BasicTokenScore.install(txHandler, ownerWallet, "Staked ICX", "sICX", BigInteger.valueOf(8), MathUtils.pow10(18));

        LOG.info("Deploying the PoolFactory");
        ConvexusFactoryScore factory = ConvexusFactoryScore.mustDeploy(txHandler, wallets[0]);

        LOG.info("Setting the pool contract bytes to Factory");
        byte[] fileContent = Files.readAllBytes(new File(Score.getFilePath("Convexus-Core:Contracts:Pool")).toPath());
        factory.setPoolContract(ownerWallet, fileContent);

        LOG.info("Creating a new SICX / USDC pool using the factory");
        Address poolAddress = factory.createPool(ownerWallet, 
            sicx.getAddress(), 
            usdc.getAddress(), 
            FEE_AMOUNTS[MEDIUM]
        );

        LOG.info("Initializing the pool");
        ConvexusPoolScore pool = new ConvexusPoolScore(new Score(txHandler, poolAddress));
        pool.initialize(ownerWallet, encodePriceSqrt(ONE, ONE));

        LOG.infoExiting();
    }

    private BigInteger encodePriceSqrt(BigInteger reserve1, BigInteger reserve0) {
        return new BigDecimal(reserve1).divide(new BigDecimal(reserve0), MathContext.DECIMAL128).sqrt(MathContext.DECIMAL128).multiply(BigDecimal.valueOf(2).pow(96)).toBigInteger();
    }
}
