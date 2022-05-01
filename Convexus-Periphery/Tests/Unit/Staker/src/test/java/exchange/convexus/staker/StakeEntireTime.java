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

import static exchange.convexus.utils.TimeUtils.ONE_DAY;
import static exchange.convexus.utils.TimeUtils.ONE_SECOND;

import java.math.BigInteger;
import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static exchange.convexus.utils.TimeUtils.now;
import static java.math.BigInteger.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import exchange.convexus.test.staker.ConvexusStakerUtils;
import exchange.convexus.utils.SleepUtils;
import score.Address;

public class StakeEntireTime extends ConvexusStakerTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  BigInteger THOUSAND = BigInteger.valueOf(1000);
  int tickSpacing = TICK_SPACINGS[MEDIUM];
  BigInteger duration = ONE_DAY.multiply(BigInteger.valueOf(30));
  BigInteger[] tokensId = new BigInteger[3];
  final BigInteger totalReward = EXA.multiply(BigInteger.valueOf(3_000));
  
  BigInteger startTime;
  BigInteger endTime;
  IncentiveKey incentiveKey;

  int[] ticksToStake = {
    getMinTick(TICK_SPACINGS[MEDIUM]),
    getMaxTick(TICK_SPACINGS[MEDIUM]),
  };

  BigInteger[] amountsToStake = {
    EXA.multiply(THOUSAND), 
    EXA.multiply(THOUSAND) 
  };

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_staker();
    setup_tokens();

    final BigInteger now = now();
    startTime = now.add(ONE_SECOND.multiply(THOUSAND));
    endTime = startTime.add(duration);

    // Create Pool
    setup_pool1();
    // setup_pool2();

    // createIncentive
    incentiveKey = ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, totalReward, staker.getAddress(), pool1.getAddress(), startTime, endTime);

    Address[] tokensToStake = {sicx.score.getAddress(), usdc.score.getAddress()};
    SleepUtils.sleep(startTime.subtract(now).divide(ONE_SECOND).intValue() + 1);
    tokensId[0] = mintDepositStakeFlow(lpUser0, tokensToStake, amountsToStake, ticksToStake, startTime, endTime, pool1.getAddress(), incentiveCreator.getAddress());
    tokensId[1] = mintDepositStakeFlow(lpUser1, tokensToStake, amountsToStake, ticksToStake, startTime, endTime, pool1.getAddress(), incentiveCreator.getAddress());
    tokensId[2] = mintDepositStakeFlow(lpUser2, tokensToStake, amountsToStake, ticksToStake, startTime, endTime, pool1.getAddress(), incentiveCreator.getAddress());
  }

  @Test
  void testAssumptions () {
    assertEquals(staker.call("factory"), factory.getAddress());
    assertEquals(staker.call("nonfungiblePositionManager"), nft.getAddress());
    assertEquals(staker.call("maxIncentiveDuration"), ONE_SECOND.multiply(TWO.pow(32)));
    assertEquals(staker.call("maxIncentiveStartLeadTime"), ONE_SECOND.multiply(TWO.pow(32)));
  }

  @Test
  void testAllowThemAllToWithdrawAtTheEnd () {
    SleepUtils.sleep(duration.divide(ONE_SECOND).intValue() + 1);

    // Sanity check: make sure we go past the incentive end time.
    assertTrue(now().compareTo(endTime) >= 0);

    // Everyone pulls their liquidity at the same time
    BigInteger[] unstakes0 = unstakeCollectBurnFlow(lpUser0, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[0]);
    BigInteger[] unstakes1 = unstakeCollectBurnFlow(lpUser1, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[1]);
    BigInteger[] unstakes2 = unstakeCollectBurnFlow(lpUser2, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[2]);

    BigInteger rewardsEarned = unstakes0[0].add(unstakes1[0]).add(unstakes2[0]);

    BigInteger amountReturnedToCreator = endIncentiveFlow(incentiveCreator, rwtk, pool1.getAddress(), startTime, endTime, incentiveCreator.getAddress());

    assertEquals(rewardsEarned.add(amountReturnedToCreator), totalReward);
  }

  @Test
  void testAllowsLpToUnstakeIfTheyHaveNotAlready () {
    SleepUtils.sleep(duration.divide(ONE_SECOND).intValue() + 1);

    // First make sure it is still owned by the staker
    assertEquals(nft.call("ownerOf", tokensId[0]), staker.getAddress());
    
    // The incentive has not yet been ended by the creator
    // It allows the token to be unstaked the first time
    unstakeToken(lpUser0, incentiveKey, tokensId[0]);

    // It does not allow them to claim rewards (since we're past end time)
    staker.invoke(lpUser0, "claimReward", rwtk.getAddress(), lpUser0.getAddress(), BigInteger.ZERO);

    // Owner is still the staker
    assertEquals(nft.call("ownerOf", tokensId[0]), staker.getAddress());

    // Now withdraw it
    staker.invoke(lpUser0, "withdrawToken", tokensId[0], lpUser0.getAddress(), "".getBytes());

    // Owner is now the LP
    assertEquals(nft.call("ownerOf", tokensId[0]), lpUser0.getAddress());
  }
}
