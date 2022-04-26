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
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;

import static exchange.convexus.utils.SleepUtils.sleep;
import static exchange.convexus.utils.TimeUtils.now;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.nft.NFTUtils;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.MathUtils;

public class CreateIncentiveTest extends ConvexusStakerTest {
  
  BigInteger[] timestamps;
  BigInteger tokenId;
  final BigInteger totalReward = EXA.multiply(BigInteger.valueOf(100));
  final BigInteger amountDesired = EXA.multiply(TEN);
  BigInteger claimable = ZERO;
  byte[] incentiveId;
  IncentiveKey incentiveKey;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_staker();
    setup_tokens();
    setup_pool1();
    setup_pool2();
    
    incentiveKey = new IncentiveKey(rwtk.getAddress(), pool1.getAddress(), ZERO, ZERO, incentiveCreator.getAddress());
  }

  void subject (Score rewardToken) {
    timestamps = makeTimestamps(now());
    ConvexusStakerUtils.createIncentive(incentiveCreator, rewardToken, totalReward, staker.getAddress(), incentiveKey.pool, timestamps[0], timestamps[1]);
  }

  void subject (BigInteger[] timestamps) {
    ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, totalReward, staker.getAddress(), incentiveKey.pool, timestamps[0], timestamps[1]);
  }

  void subject () {
    subject(rwtk.score);
  }

  @Test
  void testTransfersTheRightAmountOfRewardToken () {
    var balanceBefore = (BigInteger) rwtk.call("balanceOf", staker.getAddress());
    subject();
    var balanceAfter = (BigInteger) rwtk.call("balanceOf", staker.getAddress());
    assertEquals(balanceBefore.add(totalReward), balanceAfter);
  }

  @Test
  void testEmitsAnEventWithValidParameters () {
    timestamps = makeTimestamps(now());
    reset(staker.spy);
    subject();
    verify(staker.spy).IncentiveCreated(rwtk.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1], incentiveCreator.getAddress(), totalReward);
  }

  @Test
  void createsAnIncentiveWithTheCorrectParameters () {
    timestamps = makeTimestamps(now());
    subject();

    var incentiveId = IncentiveId.compute(
      new IncentiveKey(
        rwtk.getAddress(), 
        pool1.getAddress(), 
        timestamps[0],
        timestamps[1], 
        incentiveCreator.getAddress()
      )
    );

    var incentive = Incentive.fromMap(staker.call("incentives", incentiveId));
    assertEquals(totalReward, incentive.totalRewardUnclaimed);
    assertEquals(ZERO, incentive.totalSecondsClaimedX128);
  }
  
  @Test
  void testAddsToExistingIncentives () {
    timestamps = makeTimestamps(now());
    reset(staker.spy);
    subject();
    verify(staker.spy).IncentiveCreated(rwtk.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1], incentiveCreator.getAddress(), totalReward);
    subject();

    var incentiveId = IncentiveId.compute(
      new IncentiveKey(
        rwtk.getAddress(), 
        pool1.getAddress(), 
        timestamps[0],
        timestamps[1], 
        incentiveCreator.getAddress()
      )
    );
    
    var incentive = Incentive.fromMap(staker.call("incentives", incentiveId));

    assertEquals(totalReward.multiply(BigInteger.TWO), incentive.totalRewardUnclaimed);
    assertEquals(ZERO, incentive.totalSecondsClaimedX128);
    assertEquals(ZERO, incentive.numberOfStakes);
  }

  @Test
  void testDoesNotOverrideTheExistingNumberOfStakes () {
    BigInteger[] testTimestamps = makeTimestamps(now());
    Score rewardToken = sicx.score;
    
    incentiveKey = new IncentiveKey(rewardToken.getAddress(), pool1.getAddress(), testTimestamps[0], testTimestamps[1], incentiveCreator.getAddress());
    ConvexusStakerUtils.createIncentive(lpUser0, rewardToken, staker.getAddress(), incentiveKey, BigInteger.valueOf(100));

    var incentiveId = IncentiveId.compute(incentiveKey);
    var incentive = Incentive.fromMap(staker.call("incentives", incentiveId));

    assertEquals(BigInteger.valueOf(100), incentive.totalRewardUnclaimed);
    assertEquals(ZERO, incentive.totalSecondsClaimedX128);
    assertEquals(ZERO, incentive.numberOfStakes);
    assertEquals(BigInteger.valueOf(100), rewardToken.call("balanceOf", staker.getAddress()));

    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), sicx.score, DEFAULT_LP_AMOUNT);
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), usdc.score, DEFAULT_LP_AMOUNT);

    BigInteger tokenId = NFTUtils.mintPosition(
      nft,
      lpUser0, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(FEE_AMOUNTS[MEDIUM]),
      getMaxTick(FEE_AMOUNTS[MEDIUM]),
      DEFAULT_LP_AMOUNT,
      DEFAULT_LP_AMOUNT,
      ZERO, ZERO,
      lpUser0.getAddress(), 
      now().add(BigInteger.valueOf(1000))
    );

    NFTUtils.safeTransferFrom(nft, lpUser0, staker.getAddress(), tokenId);

    sleep(testTimestamps[0].subtract(now()));
    
    ConvexusStakerUtils.createIncentive(lpUser0, rewardToken, staker.getAddress(), incentiveKey, BigInteger.valueOf(50));
    ConvexusStakerUtils.stakeToken(staker, lpUser0, incentiveKey, tokenId);

    incentive = Incentive.fromMap(staker.call("incentives", incentiveId));
    assertEquals(BigInteger.valueOf(150), incentive.totalRewardUnclaimed);
    assertEquals(ZERO, incentive.totalSecondsClaimedX128);
    assertEquals(ONE, incentive.numberOfStakes);
  }

  @Test
  void testCurrentTimeIsAfterStartTime () {
    BigInteger[] timestamps = makeTimestamps(now(), BigInteger.valueOf(100_000));

    // Go to after the start time
    sleep(timestamps[0].add(BigInteger.valueOf(100)).subtract(now()));

    assertTrue(now().compareTo(timestamps[0]) > 0);
    assertTrue(now().compareTo(timestamps[1]) < 0);

    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      subject(timestamps), 
      "createIncentive: start time must be now or in the future");
  }

  @Test
  void testEndTimeIsBeforeStartTime () {
    BigInteger[] timestamps = makeTimestamps(now());
    timestamps[1] = timestamps[0].subtract(BigInteger.TEN);
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      subject(timestamps), 
      "createIncentive: start time must be before end time");
  }

  @Test
  void testStartTimeIsTooFarIntoTheFuture () {
    BigInteger[] timestamps = makeTimestamps(now().add(MathUtils.pow(BigInteger.TWO, 32).add(ONE)));
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      subject(timestamps), 
      "createIncentive: start time too far into future");
  }

  @Test
  void testEndTimeIsWithingValidDurationOfStartTime () {
    BigInteger[] timestamps = makeTimestamps(now());
    timestamps[1] = timestamps[0].add(MathUtils.pow(BigInteger.TWO, 32).add(ONE));

    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      subject(timestamps), 
      "createIncentive: incentive duration is too long");
  }

  @Test
  void testTotalRewardIs0OrAnInvalidAmount () {
    BigInteger[] timestamps = makeTimestamps(now(), BigInteger.valueOf(1_000));
    
    AssertUtils.assertThrowsMessage(AssertionError.class, () ->
      ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, ZERO, staker.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1]),
      "createIncentive: reward must be positive");
  }
}
