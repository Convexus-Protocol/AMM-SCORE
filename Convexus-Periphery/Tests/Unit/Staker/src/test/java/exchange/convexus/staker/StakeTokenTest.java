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

import static exchange.convexus.test.nft.NFTUtils.decreaseLiquidity;
import static exchange.convexus.test.nft.NFTUtils.safeTransferFrom;
import static exchange.convexus.test.staker.ConvexusStakerUtils.stakeToken;
import exchange.convexus.test.staker.ConvexusStakerUtils;
import static exchange.convexus.utils.SleepUtils.sleep;
import static exchange.convexus.utils.TimeUtils.now;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.test.nft.NFTUtils;
import exchange.convexus.periphery.staker.IncentiveId;
import exchange.convexus.positionmgr.PositionInformation;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.SleepUtils;

public class StakeTokenTest extends ConvexusStakerTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];
  final BigInteger amountDesired = EXA.multiply(TEN);
  final BigInteger totalReward = EXA.multiply(BigInteger.valueOf(100));
  byte[] incentiveId;
  BigInteger[] timestamps;
  BigInteger tokenId;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_staker();
    setup_tokens();
    setup_pool1();
    setup_pool2();

    // We will be doing a lot of time-testing here, so leave some room between now
    // and when the incentive starts
    timestamps = makeTimestamps(now().add(BigInteger.valueOf(1000)));

    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), sicx.score, amountDesired);
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), usdc.score, amountDesired);

    tokenId = NFTUtils.mintPosition(
      nft,
      lpUser0, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]), 
      getMaxTick(TICK_SPACINGS[MEDIUM]), 
      amountDesired, 
      amountDesired, 
      ZERO, 
      ZERO, 
      lpUser0.getAddress(), 
      now().add(BigInteger.valueOf(1000)));

    safeTransferFrom(nft, lpUser0, staker.getAddress(), tokenId);

    var incentiveKey = ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, totalReward, staker.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1]);
    incentiveId = IncentiveId.compute(incentiveKey);
  }

  void subject (BigInteger tokenId, Account from) {
    stakeToken(staker, from, rwtk.getAddress(), pool1.getAddress(), timestamps, incentiveCreator.getAddress(), tokenId);
  }

  @Test
  void testEmitsStakeEvent () {
    SleepUtils.sleep(timestamps[0].subtract(now()).add(BigInteger.valueOf(100)));
    var position = PositionInformation.fromMap(nft.call("positions", tokenId));

    reset(staker.spy);
    subject(tokenId, lpUser0);

    verify(staker.spy).TokenStaked(tokenId, incentiveId, position.liquidity);
  }

  @Test
  void testSetsTheStakeStructProperly () {
    SleepUtils.sleep(timestamps[0].subtract(now()).add(BigInteger.valueOf(100)));
    var position = PositionInformation.fromMap(nft.call("positions", tokenId));

    var stakeBefore = StakesResult.fromMap(staker.call("stakes", tokenId, incentiveId));
    var depositStakesBefore = Deposit.fromMap(staker.call("deposits", tokenId)).numberOfStakes;
    subject(tokenId, lpUser0);
    var stakeAfter = StakesResult.fromMap(staker.call("stakes", tokenId, incentiveId));
    var depositStakesAfter = Deposit.fromMap(staker.call("deposits", tokenId)).numberOfStakes;

    assertEquals(ZERO, stakeBefore.secondsPerLiquidityInsideInitialX128);
    assertEquals(ZERO, stakeBefore.liquidity);
    assertEquals(ZERO, depositStakesBefore);

    assertTrue(stakeAfter.secondsPerLiquidityInsideInitialX128.compareTo(ZERO) > 0);
    assertEquals(position.liquidity, stakeAfter.liquidity);
    assertEquals(ONE, depositStakesAfter);
  }

  @Test
  void testIncrementsTheNumberOfStakesOnTheDeposit () {
    SleepUtils.sleep(timestamps[0].subtract(now()).add(BigInteger.valueOf(100)));
    var nStakesBefore = Deposit.fromMap(staker.call("deposits", tokenId)).numberOfStakes;
    subject(tokenId, lpUser0);

    assertEquals(nStakesBefore.add(ONE), Deposit.fromMap(staker.call("deposits", tokenId)).numberOfStakes);
  }

  @Test
  void testIncrementsTheNumberOfStakesOnTheIncentive () {
    SleepUtils.sleep(timestamps[0].subtract(now()).add(BigInteger.valueOf(100)));
    BigInteger stakesBefore = Incentive.fromMap(staker.call("incentives", incentiveId)).numberOfStakes;
    subject(tokenId, lpUser0);
    BigInteger stakesAfter = Incentive.fromMap(staker.call("incentives", incentiveId)).numberOfStakes;
    assertEquals(ONE, stakesAfter.subtract(stakesBefore));
  }

  @Test
  void testDepositIsAlreadyStakedInTheIncentive () {
    SleepUtils.sleep(timestamps[0].subtract(now()).add(BigInteger.valueOf(500)));
    subject(tokenId, lpUser0);
    AssertUtils.assertThrowsMessage(AssertionError.class,
      () -> subject(tokenId, lpUser0), 
      "stakeToken: token already staked"
    ); ;
  }

  @Test
  void testNotOwnerOfTheDeposit () {
    SleepUtils.sleep(timestamps[0].subtract(now()).add(BigInteger.valueOf(500)));
    
    // lp2 instead of lp1
    AssertUtils.assertThrowsMessage(AssertionError.class,
      () -> subject(tokenId, lpUser1), 
      "stakeToken: only owner can stake token"
    );
  }

  @Test
  void testHas0LiquidityInThePosition () {
    SleepUtils.sleep(timestamps[0].subtract(now()).add(BigInteger.valueOf(500)));

    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), sicx.score, amountDesired);
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), usdc.score, amountDesired);

    BigInteger tokenId2 = NFTUtils.mintPosition(
      nft,
      lpUser0,
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]), 
      getMaxTick(TICK_SPACINGS[MEDIUM]), 
      amountDesired, 
      amountDesired, 
      ZERO, 
      ZERO, 
      lpUser0.getAddress(), 
      now().add(BigInteger.valueOf(1000))
    );

    decreaseLiquidity(
      nft, 
      lpUser0, 
      tokenId2, 
      PositionInformation.fromMap(nft.call("positions", tokenId2)).liquidity, 
      ZERO, ZERO, 
      now().add(BigInteger.valueOf(1000))
    );

    // Deposit NFT Position to Staker contract
    safeTransferFrom(nft, lpUser0, staker.getAddress(), tokenId2);

    AssertUtils.assertThrowsMessage(AssertionError.class,
      () -> subject(tokenId2, lpUser0), 
      "stakeToken: cannot stake token with 0 liquidity"
    );
  }

  @Test
  void testTokenIdIsForADifferentPoolThanTheIncentive () {
    var incentiveKey = ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, totalReward, staker.getAddress(), pool2.getAddress(), timestamps[0], timestamps[1]);

    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), usdc.score, DEFAULT_LP_AMOUNT);
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), baln.score, DEFAULT_LP_AMOUNT);

    BigInteger otherTokenId = NFTUtils.mintPosition(
      nft,
      lpUser0, 
      usdc.getAddress(), 
      baln.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]),
      getMaxTick(TICK_SPACINGS[MEDIUM]),
      DEFAULT_LP_AMOUNT,
      DEFAULT_LP_AMOUNT,
      ZERO,
      ZERO,
      lpUser0.getAddress(),
      now().add(BigInteger.valueOf(1000)));

    sleep(incentiveKey.startTime.subtract(now()).add(ONE));

    safeTransferFrom(nft, lpUser0, staker.getAddress(), otherTokenId);
    
    AssertUtils.assertThrowsMessage(AssertionError.class,
      () -> subject(otherTokenId, lpUser0), 
      "stakeToken: token pool is not the incentive pool"
    );
  }

  @Test
  void testIncentiveKeyDoesNotExist () {
    sleep(timestamps[0].add(BigInteger.valueOf(20)).subtract(now()));

    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      stakeToken(
        staker, 
        lpUser0, 
        rwtk.getAddress(), 
        pool1.getAddress(), 
        new BigInteger[] {
          timestamps[0].add(TEN), 
          timestamps[1]
        }, 
        incentiveCreator.getAddress(),
        tokenId
      ), 
      "stakeToken: non-existent incentive");
  }

  @Test
  void testIsPastTheEndTime () {
    sleep(timestamps[1].subtract(now()).add(BigInteger.valueOf(100)));
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      subject(tokenId, lpUser0),
      "stakeToken: incentive ended");
  }

  @Test
  void testIsBeforeTheStartTime () {
    sleep(timestamps[0].subtract(TEN).subtract(now()));
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      subject(tokenId, lpUser0),
      "stakeToken: incentive not started");
  }
}
