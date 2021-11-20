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

package exchange.convexus.staker;

import static exchange.convexus.utils.TimeUtils.ONE_SECOND;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import org.mockito.ArgumentCaptor;

import exchange.convexus.factory.ConvexusFactory;
import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.liquidity.ConvexusLiquidity;
import exchange.convexus.pool.ConvexusPool;
import exchange.convexus.positiondescriptor.NonfungibleTokenPositionDescriptor;
import exchange.convexus.positionmgr.MintParams;
import exchange.convexus.positionmgr.NonFungiblePositionManager;
import exchange.convexus.staker.pools.Pool01;
import exchange.convexus.staker.pools.Pool12;
import exchange.convexus.testtokens.RewardToken;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;
import exchange.convexus.utils.ConvexusTest;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.ScoreSpy;
import exchange.convexus.utils.TimeUtils;
import score.Context;

public class ConvexusStakerTest extends ConvexusTest {

  ScoreSpy<ConvexusStaker> staker;
  ScoreSpy<ConvexusFactory> factory;
  ScoreSpy<NonfungibleTokenPositionDescriptor> positiondescriptor;
  ScoreSpy<NonFungiblePositionManager> nonfungiblePositionManager;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<RewardToken> rwtk;
  ScoreSpy<ConvexusPool> pool;

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];
  protected final Account lp1 = sm.createAccount();
  protected final Account lp2 = sm.createAccount();
  protected final Account lp3 = sm.createAccount();

  protected void mintDepositStake (Account lp, Score[] tokensToStake, BigInteger[] amountsToStake, int[] ticksToStake) {
    BigInteger now = TimeUtils.nowSeconds();

    MintParams params = new MintParams();
    params.token0 = sicx.getAddress();
    params.token1 = usdc.getAddress();
    params.fee = FEE;
    params.tickLower = ticksToStake[0];
    params.tickUpper = ticksToStake[1];
    params.amount0Desired = amountsToStake[0];
    params.amount1Desired = amountsToStake[1];
    params.amount0Min = ZERO;
    params.amount1Min = ZERO;
    params.recipient = lp.getAddress();
    params.deadline = now;

    ConvexusLiquidity.deposit(lp, nonfungiblePositionManager.getAddress(), sicx.score, new BigInteger("999999999999999999891"));
    ConvexusLiquidity.deposit(lp, nonfungiblePositionManager.getAddress(), usdc.score, new BigInteger("999999999999999999891"));
    nonfungiblePositionManager.invoke(lp, "mint", params);
    
    // Get IncreaseLiquidity event
    ArgumentCaptor<BigInteger> _tokenId = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _liquidity = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(nonfungiblePositionManager.spy).IncreaseLiquidity(_tokenId.capture(), _liquidity.capture(), _amount0.capture(), _amount1.capture());
    BigInteger tokenId = _tokenId.getValue();

    // The LP approves and stakes their NFT
    nonfungiblePositionManager.invoke(lp, "approve", staker.getAddress(), tokenId);

    // Deposit
    nonfungiblePositionManager.invoke(lp, "safeTransferFrom", lp.getAddress(), staker.getAddress(), tokenId, "safeTransferFrom".getBytes());
  }

  void setup_tokens () throws Exception {
    sicx = deploy_sicx();
    usdc = deploy_usdc();
    rwtk = deploy_reward_token();
    
    // Transfer some funds to Alice
    sicx.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    rwtk.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    // Transfer some funds to Bob
    sicx.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    rwtk.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    
    // Transfer some funds to LPs
    sicx.invoke(owner, "mintTo", lp1.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", lp1.getAddress(), IntUtils.MAX_UINT256);
    rwtk.invoke(owner, "mintTo", lp1.getAddress(), IntUtils.MAX_UINT256);
    
    sicx.invoke(owner, "mintTo", lp2.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", lp2.getAddress(), IntUtils.MAX_UINT256);
    rwtk.invoke(owner, "mintTo", lp2.getAddress(), IntUtils.MAX_UINT256);
    
    sicx.invoke(owner, "mintTo", lp3.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", lp3.getAddress(), IntUtils.MAX_UINT256);
    rwtk.invoke(owner, "mintTo", lp3.getAddress(), IntUtils.MAX_UINT256);
  }

  void setup_pool01 () throws Exception {
    pool = deploy(Pool01.class, sicx.getAddress(), usdc.getAddress(), factory.getAddress(), FEE, tickSpacing);
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE, pool.getAddress());
  }

  void setup_pool12 () throws Exception {
    pool = deploy(Pool12.class, usdc.getAddress(), rwtk.getAddress(), factory.getAddress(), FEE, tickSpacing);
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    ConvexusFactoryUtils.createPool(factory, alice, usdc.getAddress(), rwtk.getAddress(), FEE, pool.getAddress());
  }

  void setup_staker () throws Exception {
    factory = deploy_factory();
    positiondescriptor = deploy_positiondescriptor();
    nonfungiblePositionManager = deploy_positionmgr(factory.getAddress(), positiondescriptor.getAddress());
    staker = deploy_staker(
      factory.getAddress(), 
      nonfungiblePositionManager.getAddress(), 
      ONE_SECOND.multiply(TWO.pow(32)), 
      ONE_SECOND.multiply(TWO.pow(32)));
  }
}
