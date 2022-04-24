/*
 * Copyright 2022 ICONation
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

import com.iconloop.score.test.Account;
import com.iconloop.score.test.ServiceManager;

import static exchange.convexus.utils.SleepUtils.sleep;
import static exchange.convexus.utils.TimeUtils.now;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.nft.NFTUtils;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.SleepUtils;

public class UnstakeTokenTest extends ConvexusStakerTest {
  
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
    
    timestamps = makeTimestamps(now());
    
    ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, totalReward, staker.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1]);
    
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

      
    NFTUtils.safeTransferFrom(nft, lpUser0, staker.getAddress(), tokenId);

    SleepUtils.sleep(timestamps[0].subtract(now()).add(ONE));
    
    incentiveKey = new IncentiveKey(
      rwtk.getAddress(), 
      pool1.getAddress(), 
      timestamps[0], 
      timestamps[1], 
      incentiveCreator.getAddress()
    );
    ConvexusStakerUtils.stakeToken(staker, lpUser0, incentiveKey, tokenId);

    incentiveId = IncentiveId.compute(incentiveKey);
  }

  void subject (Account from) {
    staker.invoke(from, "unstakeToken", incentiveKey, tokenId);
  }

  // when requesting the full amount
  @Test
  void testDecrementsDepositNumberOfStakesBy1 () {
    var stakesPre = Deposit.fromMap(staker.call("deposits", tokenId)).numberOfStakes;
    subject(lpUser0);
    var stakesPost = Deposit.fromMap(staker.call("deposits", tokenId)).numberOfStakes;
    assertEquals(stakesPre.subtract(ONE), stakesPost);
  }

  @Test
  void testDecrementsIncentiveNumberOfStakesBy1 () {
    var stakesPre = Incentive.fromMap(staker.call("incentives", incentiveId)).numberOfStakes;
    subject(lpUser0);
    var stakesPost = Incentive.fromMap(staker.call("incentives", incentiveId)).numberOfStakes;
    assertEquals(stakesPre.subtract(ONE), stakesPost);
  }

  @Test
  void testEmitsUnstakedEvent () {
    reset(staker.spy);
    subject(lpUser0);
    verify(staker.spy).TokenUnstaked(tokenId, incentiveId);
  }

  @Test
  void testUpdatesTheRewardAvailableForTheStaker() {
    SleepUtils.sleep(1);
    BigInteger rewardsAccuredBefore = (BigInteger) staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress());
    subject(lpUser0);
    BigInteger rewardsAccuredAfter = (BigInteger) staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress());
    assertTrue(rewardsAccuredAfter.compareTo(rewardsAccuredBefore) > 0);
  }

  @Test
  void testUpdatesTheStakeStruct () {
    var stakeBefore = StakesResult.fromMap(staker.call("stakes", tokenId, incentiveId));
    subject(lpUser0);
    var stakeAfter = StakesResult.fromMap(staker.call("stakes", tokenId, incentiveId));

    assertTrue(stakeBefore.secondsPerLiquidityInsideInitialX128.compareTo(ZERO) > 0);
    assertTrue(stakeBefore.liquidity.compareTo(ZERO) > 0);
    assertEquals(ZERO, stakeAfter.secondsPerLiquidityInsideInitialX128);
    assertEquals(ZERO, stakeAfter.liquidity);
  }

  @Test
  void testAnyoneCanUnstake () {
    // Fast-forward to after the end time
    sleep(timestamps[1].add(ONE).subtract(now()));
    subject(lpUser1);
  }

  @Test
  void testOwnerCanUnstake () {
    // Fast-forward to after the end time
    sleep(timestamps[1].add(ONE).subtract(now()));
    subject(lpUser0);
  }

  @Test
  void testStakeHasAlreadyBeenUnstaked () {
    sleep(timestamps[1].add(ONE).subtract(now()));
    subject(lpUser0);
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> subject(lpUser0), 
      "unstakeToken: stake does not exist");
  }

  @Test
  void testYouHaveNotStaked () {
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> subject(lpUser2), 
      "unstakeToken: only owner can withdraw token before incentive end time");
  }

  @Test
  void testNonOwnerTriesToUnstakeBeforeTheEndTime () {
    var nonOwner = lpUser2;
    
    sleep(timestamps[0].add(BigInteger.valueOf(100)).subtract(now()));
    AssertUtils.assertThrowsMessage(AssertionError.class, () -> subject(nonOwner), 
      "unstakeToken: only owner can withdraw token before incentive end time");

    assertTrue(now().compareTo(timestamps[1]) < 0);
  }
}
