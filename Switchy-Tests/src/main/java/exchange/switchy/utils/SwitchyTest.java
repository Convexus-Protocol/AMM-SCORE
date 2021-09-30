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

package exchange.switchy.utils;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import exchange.switchy.pool.SwitchyPool;
import exchange.switchy.factory.SwitchyFactory;
import exchange.switchy.initializer.SwitchyPoolInitializer;
import exchange.switchy.router.SwapRouter;
import exchange.switchy.ticklens.TickLens;
import exchange.switchy.liquidity.SwitchyLiquidityManagement;
import exchange.switchy.quoter.Quoter;
import exchange.switchy.pairflash.PairFlash;

import static org.mockito.Mockito.spy;

import java.math.BigInteger;

public class SwitchyTest extends TestBase {

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
    public ScoreSpy<SwitchyPool> deploy_pool () throws Exception {
        Score score = sm.deploy(owner, SwitchyPool.class);

        var spy = (SwitchyPool) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<SwitchyPool>(score, spy);
    }
    
    public ScoreSpy<SwitchyFactory> deploy_factory () throws Exception {
        Score score = sm.deploy(owner, SwitchyFactory.class);

        var spy = (SwitchyFactory) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<SwitchyFactory>(score, spy);
    }
    
    public ScoreSpy<SwapRouter> deploy_router () throws Exception {
        Score score = sm.deploy(owner, SwapRouter.class);

        var spy = (SwapRouter) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<SwapRouter>(score, spy);
    }
    
    public ScoreSpy<SwitchyPoolInitializer> deploy_initializer () throws Exception {
        Score score = sm.deploy(owner, SwitchyPoolInitializer.class);

        var spy = (SwitchyPoolInitializer) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<SwitchyPoolInitializer>(score, spy);
    }
    
    public ScoreSpy<SwitchyLiquidityManagement> deploy_liquidity_management () throws Exception {
        Score score = sm.deploy(owner, SwitchyLiquidityManagement.class);

        var spy = (SwitchyLiquidityManagement) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<SwitchyLiquidityManagement>(score, spy);
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
}
