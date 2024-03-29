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

package exchange.convexus.test;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import exchange.convexus.core.pool.contracts.ConvexusPool;
import exchange.convexus.mocks.factory.ConvexusFactoryMock;
import exchange.convexus.periphery.cxs.CXS;
import exchange.convexus.periphery.governor.ConvexusGovernor;
import exchange.convexus.periphery.initializer.ConvexusPoolInitializer;
import exchange.convexus.periphery.liquidity.ConvexusLiquidityManagement;
import exchange.convexus.periphery.pairflash.PairFlash;
import exchange.convexus.periphery.poolreadonly.ConvexusPoolReadOnly;
import exchange.convexus.periphery.positiondescriptor.NonfungibleTokenPositionDescriptor;
import exchange.convexus.periphery.positionmgr.NonFungiblePositionManager;
import exchange.convexus.periphery.quoter.Quoter;
import exchange.convexus.periphery.router.SwapRouter;
import exchange.convexus.periphery.staker.ConvexusStaker;
import exchange.convexus.periphery.swap.Swap;
import exchange.convexus.periphery.ticklens.TickLens;
import exchange.convexus.test.contracts.callee.ConvexusPoolCallee;
import exchange.convexus.test.contracts.reentrantcallee.ConvexusReentrantCallee;
import exchange.convexus.test.contracts.swappay.ConvexusSwapPay;
import exchange.convexus.test.factory.ConvexusFactoryUtils;
import exchange.convexus.mocks.pool.ConvexusPoolMock;
import exchange.convexus.test.tokens.Baln;
import exchange.convexus.test.tokens.RewardToken;
import exchange.convexus.test.tokens.Sicx;
import exchange.convexus.test.tokens.Usdc;
import exchange.convexus.utils.ScoreSpy;
import score.Address;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class ConvexusTest extends TestBase {

    protected final static ServiceManager sm = getServiceManager();

    // Roles
    protected final static Account owner = sm.createAccount(10000);
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

    // Encode price
    protected BigInteger encodePriceSqrt (BigInteger reserve1, BigInteger reserve0) {
        return new BigDecimal(reserve1).divide(new BigDecimal(reserve0), MathContext.DECIMAL128).sqrt(MathContext.DECIMAL128).multiply(BigDecimal.valueOf(2).pow(96)).toBigInteger();
    }
    protected BigInteger encodePriceSqrt (int reserve1, int reserve0) {
        return encodePriceSqrt(BigInteger.valueOf(reserve1), BigInteger.valueOf(reserve0));
    }

    // Tick
    protected int getMinTick(int tickSpacing) {
        return ((int) Math.ceil(-887272 / tickSpacing)) * tickSpacing;
    }

    protected int getMaxTick(int tickSpacing) {
        return ((int) Math.floor(887272 / tickSpacing)) * tickSpacing;
    }

    // Deployers
    public static <T> ScoreSpy<T> deploy (Class<?> clazz, Object... params) throws Exception {
        Score score = sm.deploy(owner, clazz, params);

        @SuppressWarnings("unchecked")
        var spy = (T) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<T>(score, spy);
    }

    public ScoreSpy<ConvexusPool> deploy_pool (Address token0, Address token1, Address factory, int fee, int tickSpacing) throws Exception {
        Score score = sm.deploy(owner, ConvexusPool.class, token0, token1, factory, fee, tickSpacing);

        var spy = (ConvexusPool) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusPool>(score, spy);
    }
    
    public ScoreSpy<ConvexusFactoryMock> deploy_factory () throws Exception {
        Score score = sm.deploy(owner, ConvexusFactoryMock.class);

        var spy = (ConvexusFactoryMock) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusFactoryMock>(score, spy);
    }
    
    public ScoreSpy<ConvexusPoolReadOnly> deploy_pool_readonly () throws Exception {
        Score score = sm.deploy(owner, ConvexusPoolReadOnly.class);

        var spy = (ConvexusPoolReadOnly) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusPoolReadOnly>(score, spy);
    }
    
    public ScoreSpy<SwapRouter> deploy_router (Address factory) throws Exception {
        Score score = sm.deploy(owner, SwapRouter.class, factory);

        var spy = (SwapRouter) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<SwapRouter>(score, spy);
    }
    
    public ScoreSpy<ConvexusPoolInitializer> deploy_initializer (Address factory, Address nft) throws Exception {
        Score score = sm.deploy(owner, ConvexusPoolInitializer.class, factory, nft);

        var spy = (ConvexusPoolInitializer) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusPoolInitializer>(score, spy);
    }
    
    public ScoreSpy<ConvexusGovernor> deploy_governor () throws Exception {
        Score score = sm.deploy(owner, ConvexusGovernor.class);

        var spy = (ConvexusGovernor) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusGovernor>(score, spy);
    }
    
    public ScoreSpy<ConvexusLiquidityManagement> deploy_liquidity_management () throws Exception {
        Score score = sm.deploy(owner, ConvexusLiquidityManagement.class);

        var spy = (ConvexusLiquidityManagement) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusLiquidityManagement>(score, spy);
    }
    
    public ScoreSpy<CXS> deploy_cxs () throws Exception {
        Score score = sm.deploy(owner, CXS.class);

        var spy = (CXS) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<CXS>(score, spy);
    }
    
    public ScoreSpy<NonFungiblePositionManager> deploy_nft (Address factory, Address tokenDescriptor) throws Exception {
        Score score = sm.deploy(owner, NonFungiblePositionManager.class, factory, tokenDescriptor);

        var spy = (NonFungiblePositionManager) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<NonFungiblePositionManager>(score, spy);
    }
    
    public ScoreSpy<NonfungibleTokenPositionDescriptor> deploy_positiondescriptor () throws Exception {
        Score score = sm.deploy(owner, NonfungibleTokenPositionDescriptor.class);

        var spy = (NonfungibleTokenPositionDescriptor) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<NonfungibleTokenPositionDescriptor>(score, spy);
    }
    
    public ScoreSpy<TickLens> deploy_ticklens () throws Exception {
        Score score = sm.deploy(owner, TickLens.class);

        var spy = (TickLens) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<TickLens>(score, spy);
    }
    
    public ScoreSpy<Quoter> deploy_quoter (Address factory, Address readOnlyPool) throws Exception {
        Score score = sm.deploy(owner, Quoter.class, factory, readOnlyPool);

        var spy = (Quoter) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<Quoter>(score, spy);
    }
    
    public ScoreSpy<PairFlash> deploy_flash (Address router, Address factory) throws Exception {
        Score score = sm.deploy(owner, PairFlash.class, router, factory);

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
    
    public ScoreSpy<ConvexusStaker> deploy_staker (
        Address _factory,
        Address _nonfungiblePositionManager,
        BigInteger _maxIncentiveStartLeadTime,
        BigInteger _maxIncentiveDuration
    ) throws Exception {
        Score score = sm.deploy(owner, ConvexusStaker.class, _factory, _nonfungiblePositionManager, _maxIncentiveStartLeadTime, _maxIncentiveDuration);

        var spy = (ConvexusStaker) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusStaker>(score, spy);
    }

    public ScoreSpy<Sicx> deploy_sicx () throws Exception {
        Score score = sm.deploy(owner, Sicx.class, "Staked ICX", "sICX", 18);

        var spy = (Sicx) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<Sicx>(score, spy);
    }

    public ScoreSpy<Usdc> deploy_usdc () throws Exception {
        Score score = sm.deploy(owner, Usdc.class, "USDC", "USDC", 6);

        var spy = (Usdc) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<Usdc>(score, spy);
    }

    public ScoreSpy<Baln> deploy_baln () throws Exception {
        Score score = sm.deploy(owner, Baln.class, "BALN", "BALN", 18);

        var spy = (Baln) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<Baln>(score, spy);
    }
    
    public ScoreSpy<RewardToken> deploy_reward_token () throws Exception {
        Score score = sm.deploy(owner, RewardToken.class, "RewardToken", "RWT", 18);

        var spy = (RewardToken) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<RewardToken>(score, spy);
    }
    
    public ScoreSpy<ConvexusPoolCallee> deploy_callee () throws Exception {
        Score score = sm.deploy(owner, ConvexusPoolCallee.class);
        var spy = (ConvexusPoolCallee) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusPoolCallee>(score, spy);
    }
    
    public ScoreSpy<ConvexusReentrantCallee> deploy_reentrant_callee () throws Exception {
        Score score = sm.deploy(owner, ConvexusReentrantCallee.class);
        var spy = (ConvexusReentrantCallee) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusReentrantCallee>(score, spy);
    }
    
    public ScoreSpy<ConvexusSwapPay> deploy_swap_pay () throws Exception {
        Score score = sm.deploy(owner, ConvexusSwapPay.class);
        var spy = (ConvexusSwapPay) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusSwapPay>(score, spy);
    }
    
    public ScoreSpy<ConvexusPoolMock> deploy_mock_pool (Address token0, Address token1, Address factory, int fee, int tickSpacing) throws Exception {
        Score score = sm.deploy(owner, ConvexusPoolMock.class, token0, token1, factory, fee, tickSpacing);

        var spy = (ConvexusPoolMock) spy(score.getInstance());
        score.setInstance(spy);
        return new ScoreSpy<ConvexusPoolMock>(score, spy);
    }
    
    // Mock createAndInitializePoolIfNecessary until SCORE deployers supported by unittest
    public static ScoreSpy<?> createAndInitializePoolIfNecessary (
        Class<?> poolClass, 
        Account from, 
        ScoreSpy<ConvexusFactoryMock> factory,
        Address token0, 
        Address token1, 
        int fee, 
        BigInteger price, 
        int tickSpacing
    ) {
        try {
            ScoreSpy<?> pool = ConvexusTest.deploy(poolClass, token0, token1, factory.getAddress(), fee, tickSpacing);
            pool.invoke(from, "initialize", price);
            ConvexusFactoryUtils.createPool(factory, from, token0, token1, fee, pool.getAddress());
            return pool;
        } catch (Exception e) {
            assertEquals(e.getMessage(), "");
        }

        return null;
    }
}
