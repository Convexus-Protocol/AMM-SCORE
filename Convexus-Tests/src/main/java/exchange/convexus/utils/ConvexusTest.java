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

package exchange.convexus.utils;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import exchange.convexus.pool.ConvexusPool;
import exchange.convexus.factory.ConvexusFactory;
import exchange.convexus.initializer.ConvexusPoolInitializer;
import exchange.convexus.router.SwapRouter;
import exchange.convexus.ticklens.TickLens;
import score.Address;
import exchange.convexus.liquidity.ConvexusLiquidityManagement;
import exchange.convexus.quoter.Quoter;
import exchange.convexus.pairflash.PairFlash;
import exchange.convexus.swap.Swap;
import exchange.convexus.testtokens.Bnusd;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;
import exchange.convexus.callee.ConvexusCallee;

import static org.mockito.Mockito.spy;

import java.math.BigInteger;

public class ConvexusTest extends TestBase {

    protected final ServiceManager sm = getServiceManager();

    // Roles
    protected final Account owner = sm.createAccount();
    protected final Account minter = sm.createAccount();
    protected final Account burner = sm.createAccount();
    protected final Account admin = sm.createAccount();

    // User accounts
    protected final Account alice = sm.createAccount();
    protected final Account bob = sm.createAccount();
    protected final Account charlie = sm.createAccount();
    protected final Account eve = sm.createAccount();

    // BigInteger utils
    protected final BigInteger EXA = BigInteger.TEN.pow(18);

    // Deployers
    public ScoreSpy<ConvexusPool> deploy_pool (Address token0, Address token1, Address factory, int fee, int tickSpacing) throws Exception {
        Score score = sm.deploy(owner, ConvexusPool.class, token0, token1, factory, fee, tickSpacing);

        var spy = (ConvexusPool) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusPool>(score, spy);
    }
    
    public ScoreSpy<ConvexusFactory> deploy_factory () throws Exception {
        Score score = sm.deploy(owner, ConvexusFactory.class);

        var spy = (ConvexusFactory) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusFactory>(score, spy);
    }
    
    public ScoreSpy<SwapRouter> deploy_router () throws Exception {
        Score score = sm.deploy(owner, SwapRouter.class);

        var spy = (SwapRouter) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<SwapRouter>(score, spy);
    }
    
    public ScoreSpy<ConvexusPoolInitializer> deploy_initializer () throws Exception {
        Score score = sm.deploy(owner, ConvexusPoolInitializer.class);

        var spy = (ConvexusPoolInitializer) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusPoolInitializer>(score, spy);
    }
    
    public ScoreSpy<ConvexusLiquidityManagement> deploy_liquidity_management () throws Exception {
        Score score = sm.deploy(owner, ConvexusLiquidityManagement.class);

        var spy = (ConvexusLiquidityManagement) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusLiquidityManagement>(score, spy);
    }
    
    public ScoreSpy<TickLens> deploy_ticklens () throws Exception {
        Score score = sm.deploy(owner, TickLens.class);

        var spy = (TickLens) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<TickLens>(score, spy);
    }
    
    public ScoreSpy<Quoter> deploy_quoter () throws Exception {
        Score score = sm.deploy(owner, Quoter.class);

        var spy = (Quoter) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<Quoter>(score, spy);
    }
    
    public ScoreSpy<PairFlash> deploy_pairflash () throws Exception {
        Score score = sm.deploy(owner, PairFlash.class);

        var spy = (PairFlash) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<PairFlash>(score, spy);
    }

    public ScoreSpy<Swap> deploy_swap () throws Exception {
        Score score = sm.deploy(owner, Swap.class);

        var spy = (Swap) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<Swap>(score, spy);
    }

    public ScoreSpy<Sicx> deploy_sicx () throws Exception {
        Score score = sm.deploy(owner, Sicx.class);

        var spy = (Sicx) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<Sicx>(score, spy);
    }
    
    public ScoreSpy<Bnusd> deploy_bnusd () throws Exception {
        Score score = sm.deploy(owner, Bnusd.class);

        var spy = (Bnusd) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<Bnusd>(score, spy);
    }
    
    public ScoreSpy<Usdc> deploy_usdc () throws Exception {
        Score score = sm.deploy(owner, Usdc.class);

        var spy = (Usdc) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<Usdc>(score, spy);
    }
    
    public ScoreSpy<ConvexusCallee> deploy_callee () throws Exception {
        Score score = sm.deploy(owner, ConvexusCallee.class);
        var spy = (ConvexusCallee) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusCallee>(score, spy);
    }
}