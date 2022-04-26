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

package exchange.convexus.staker;

import static exchange.convexus.utils.TimeUtils.ONE_SECOND;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import exchange.convexus.factory.ConvexusFactoryMock;
import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.nft.NFTUtils;
import exchange.convexus.periphery.positiondescriptor.NonfungibleTokenPositionDescriptor;
import exchange.convexus.periphery.positionmgr.DecreaseLiquidityParams;
import exchange.convexus.periphery.positionmgr.NonFungiblePositionManager;
import exchange.convexus.periphery.staker.ConvexusStaker;
import exchange.convexus.pools.Pool1;
import exchange.convexus.pools.Pool2;
import exchange.convexus.positionmgr.PositionInformation;
import exchange.convexus.testtokens.RewardToken;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;
import exchange.convexus.testtokens.Baln;
import exchange.convexus.utils.ConvexusTest;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.ScoreSpy;
import static exchange.convexus.utils.TimeUtils.now;
import score.Address;

public class ConvexusStakerTest extends ConvexusTest {

  ScoreSpy<ConvexusStaker> staker;
  ScoreSpy<ConvexusFactoryMock> factory;
  ScoreSpy<NonfungibleTokenPositionDescriptor> positiondescriptor;
  ScoreSpy<NonFungiblePositionManager> nft;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<Baln> baln;
  ScoreSpy<RewardToken> rwtk;
  ScoreSpy<Pool1> pool1;
  ScoreSpy<Pool2> pool2;

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  protected final Account lpUser0 = sm.createAccount();
  protected final Account lpUser1 = sm.createAccount();
  protected final Account lpUser2 = sm.createAccount();
  protected final Account incentiveCreator = sm.createAccount();
  final BigInteger DEFAULT_LP_AMOUNT = EXA.multiply(TEN);

  void setup_tokens () throws Exception {
    sicx = deploy_sicx();
    usdc = deploy_usdc();
    baln = deploy_baln();
    rwtk = deploy_reward_token();

    // Transfer some funds to Alice
    sicx.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    baln.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    rwtk.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    // Transfer some funds to Bob
    sicx.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    baln.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    rwtk.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    // Transfer some funds to incentiveCreator
    sicx.invoke(owner, "mintTo", incentiveCreator.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", incentiveCreator.getAddress(), IntUtils.MAX_UINT256);
    baln.invoke(owner, "mintTo", incentiveCreator.getAddress(), IntUtils.MAX_UINT256);
    rwtk.invoke(owner, "mintTo", incentiveCreator.getAddress(), IntUtils.MAX_UINT256);
    
    // Transfer some funds to LPs
    sicx.invoke(owner, "mintTo", lpUser0.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", lpUser0.getAddress(), IntUtils.MAX_UINT256);
    baln.invoke(owner, "mintTo", lpUser0.getAddress(), IntUtils.MAX_UINT256);
    
    sicx.invoke(owner, "mintTo", lpUser1.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", lpUser1.getAddress(), IntUtils.MAX_UINT256);
    baln.invoke(owner, "mintTo", lpUser1.getAddress(), IntUtils.MAX_UINT256);
    
    sicx.invoke(owner, "mintTo", lpUser2.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", lpUser2.getAddress(), IntUtils.MAX_UINT256);
    baln.invoke(owner, "mintTo", lpUser2.getAddress(), IntUtils.MAX_UINT256);
  }

  void setup_pool1 () throws Exception {
    pool1 = deploy(Pool1.class, sicx.getAddress(), usdc.getAddress(), factory.getAddress(), FEE_AMOUNTS[MEDIUM], TICK_SPACINGS[MEDIUM]);
    pool1.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], pool1.getAddress());
  }
  
  void setup_pool2 () throws Exception {
    pool2 = deploy(Pool2.class, usdc.getAddress(), baln.getAddress(), factory.getAddress(), FEE_AMOUNTS[MEDIUM], TICK_SPACINGS[MEDIUM]);
    pool2.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    ConvexusFactoryUtils.createPool(factory, alice, usdc.getAddress(), baln.getAddress(), FEE_AMOUNTS[MEDIUM], pool2.getAddress());
  }

  void setup_staker () throws Exception {
    factory = deploy_factory();
    positiondescriptor = deploy_positiondescriptor();
    nft = deploy_nft(factory.getAddress(), positiondescriptor.getAddress());
    staker = deploy_staker(
      factory.getAddress(), 
      nft.getAddress(), 
      ONE_SECOND.multiply(TWO.pow(32)), 
      ONE_SECOND.multiply(TWO.pow(32))
    );
  }

  // --- Helpers ---
  protected BigInteger mintDepositStake (Account lp, Address[] tokensToStake, BigInteger[] amountsToStake, int[] ticksToStake, BigInteger startTime, BigInteger endTime, Address poolAddress, Address refundee) {
    ConvexusLiquidityUtils.deposit(lp, nft.getAddress(), sicx.score, amountsToStake[0]);
    ConvexusLiquidityUtils.deposit(lp, nft.getAddress(), usdc.score, amountsToStake[1]);

    // The LP mints their NFT
    BigInteger tokenId = NFTUtils.mintPosition(
      nft,
      lp, 
      tokensToStake[0], 
      tokensToStake[1], 
      FEE_AMOUNTS[MEDIUM], 
      ticksToStake[0],
      ticksToStake[1],
      amountsToStake[0],
      amountsToStake[1],
      ZERO, ZERO,
      lp.getAddress(), 
      now().add(BigInteger.valueOf(1000))
    );

    // The LP approves and stakes their NFT
    nft.invoke(lp, "approve", staker.getAddress(), tokenId);
    NFTUtils.safeTransferFrom(nft, lp, staker.getAddress(), tokenId);

    // Stake
    ConvexusStakerUtils.stakeToken(staker, lp, rwtk.getAddress(), poolAddress, new BigInteger[] {startTime, endTime}, refundee, tokenId);

    return tokenId;
  }

  protected void unstakeCollectBurnFlow (
    Account lp, 
    Address rewardToken,
    Address pool,
    BigInteger startTime,
    BigInteger endTime,
    BigInteger tokenId
  ) {
    unstakeToken(lp, new IncentiveKey(rwtk.getAddress(), pool, startTime, endTime, alice.getAddress()), tokenId);
    // final BigInteger unstakedAt = TimeUtils.nowSeconds();

    staker.invoke(lp, "claimReward", rwtk.getAddress(), lp.getAddress(), BigInteger.ZERO);

    var position = PositionInformation.fromMap(nft.call("positions", tokenId));

    final BigInteger THOUSAND = BigInteger.valueOf(1000);
    nft.invoke(lp, "decreaseLiquidity", new DecreaseLiquidityParams(tokenId, position.liquidity, ZERO, ZERO, now().add(THOUSAND)));
  }

  protected void unstakeToken(Account from, IncentiveKey incentiveKey, BigInteger tokenId) {
    staker.invoke(from, "unstakeToken", incentiveKey, tokenId);
  }
  
  protected BigInteger[] makeTimestamps(BigInteger n, BigInteger duration) {
    return new BigInteger[] {
      n.add(BigInteger.valueOf(100)),
      n.add(BigInteger.valueOf(100).add(duration))
    };
  }

  protected BigInteger[] makeTimestamps(BigInteger n) {
    return makeTimestamps(n, BigInteger.valueOf(1000));
  }
}
