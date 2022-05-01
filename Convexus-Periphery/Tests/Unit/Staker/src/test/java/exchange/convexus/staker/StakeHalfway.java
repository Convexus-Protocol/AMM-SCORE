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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static exchange.convexus.utils.TimeUtils.now;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import exchange.convexus.test.staker.ConvexusStakerUtils;
import exchange.convexus.utils.SleepUtils;
import score.Address;

public class StakeHalfway extends ConvexusStakerTest {

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
  void testOnlyGivesThemOneSixthTheTotalReward () {
      // Halfway through, lp0 decides they want out. Pauvre lp0.
    SleepUtils.sleepTo(startTime.add(duration.divide(TWO)));

    BigInteger[] unstakes0 = unstakeCollectBurnFlow(lpUser0, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[0]);
    
    /**
     * totalReward is 3000e18
     *
     * This user contributed 1/3 of the total liquidity (amountsToStake = 1000e18)
     * for the first half of the duration, then unstaked.
     *
     * So that's (1/3)*(1/2)*3000e18 = 50e18
     */
    assertEquals(unstakes0[0], new BigInteger("499999999999999999999"));

    // Now the other two LPs hold off till the end and unstake
      // Halfway through, lp0 decides they want out. Pauvre lp0.
    SleepUtils.sleep(endTime.subtract(now()).divide(ONE_SECOND).intValue() + 1);
    
    BigInteger[] unstakes1 = unstakeCollectBurnFlow(lpUser1, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[1]);
    BigInteger[] unstakes2 = unstakeCollectBurnFlow(lpUser2, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[2]);

    // We don't need this call anymore because we're already setting that time above
    BigInteger amountReturnedToCreator = endIncentiveFlow(incentiveCreator, rwtk, pool1.getAddress(), startTime, endTime, incentiveCreator.getAddress());
    
    /* lpUser{1,2} should each have 5/12 of the total rewards.
      (1/3 * 1/2) from before lpUser0 withdrew
      (1/2 * 1/2) from after lpUser0. 
    */
    var ratio1 = new BigDecimal(unstakes1[0]).divide(new BigDecimal(unstakes0[0]), MathContext.DECIMAL32).setScale(2, RoundingMode.CEILING);
    var ratio2 = new BigDecimal(unstakes2[0]).divide(new BigDecimal(unstakes1[0]), MathContext.DECIMAL32).setScale(2, RoundingMode.CEILING);

    assertEquals(ratio1, new BigDecimal("2.50"));
    assertEquals(ratio2, new BigDecimal("1.00"));

    // All should add up to totalReward
    BigInteger rewardsEarned = unstakes0[0].add(unstakes1[0]).add(unstakes2[0]);
    assertEquals(rewardsEarned.add(amountReturnedToCreator), totalReward);
  }

  @Test
  void testRestakesAt34Mark () {
    final BigInteger THREE = BigInteger.valueOf(3);
    final BigInteger FOUR = BigInteger.valueOf(4);
    // lpUser0 unstakes at the halfway mark
    SleepUtils.sleepTo(startTime.add(duration.divide(TWO)));
    
    BigInteger[] unstakes0 = unstakeCollectBurnFlow(lpUser0, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[0]);

    // lpUser0 then restakes at the 3/4 mark
    SleepUtils.sleepTo(startTime.add(duration.multiply(THREE).divide(FOUR)));
    Address[] tokensToStake = {sicx.score.getAddress(), usdc.score.getAddress()};
    BigInteger restake = mintDepositStakeFlow(lpUser0, tokensToStake, amountsToStake, ticksToStake, startTime, endTime, pool1.getAddress(), incentiveCreator.getAddress());
    
    SleepUtils.sleepTo(endTime.add(ONE));
    
    BigInteger[] unstakes1 = unstakeCollectBurnFlow(lpUser0, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, restake);
    assertEquals(unstakes1[0], new BigInteger("750000154320844764649"));
  }

  @Test
  void testWhenAnotherLPStartsStakingHalfway () {
    // Halfway through, lp3 decides they want in. Good for them.
    final Account lpUser3 = Account.newExternalAccount(1337);
    SleepUtils.sleepTo(startTime.add(duration.divide(TWO)));
    Address[] tokensToStake = {sicx.score.getAddress(), usdc.score.getAddress()};
      
    BigInteger[] halfAmountsToStake = {
      amountsToStake[0].divide(TWO), 
      amountsToStake[1].divide(TWO) 
    };
    sicx.invoke(owner, "mintTo", lpUser3.getAddress(), halfAmountsToStake[0]);
    usdc.invoke(owner, "mintTo", lpUser3.getAddress(), halfAmountsToStake[1]);

    BigInteger extraStake = mintDepositStakeFlow(lpUser3, tokensToStake, halfAmountsToStake, ticksToStake, startTime, endTime, pool1.getAddress(), incentiveCreator.getAddress());

    // Now, go to the end and get rewards
    SleepUtils.sleepTo(endTime.add(ONE));
    
    BigInteger[] unstakes0 = unstakeCollectBurnFlow(lpUser0, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[0]);
    BigInteger[] unstakes1 = unstakeCollectBurnFlow(lpUser1, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[1]);
    BigInteger[] unstakes2 = unstakeCollectBurnFlow(lpUser2, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[2]);
    BigInteger[] unstakes3 = unstakeCollectBurnFlow(lpUser3, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, extraStake);

    var ratio = new BigDecimal(unstakes2[0]).divide(new BigDecimal(unstakes3[0]), MathContext.DECIMAL32).setScale(2, RoundingMode.CEILING);
    assertEquals(ratio, new BigDecimal("4.34"));
    
    BigInteger rewardsEarned = unstakes0[0].add(unstakes1[0]).add(unstakes2[0]).add(unstakes3[0]);

    BigInteger amountReturnedToCreator = endIncentiveFlow(incentiveCreator, rwtk, pool1.getAddress(), startTime, endTime, incentiveCreator.getAddress());
    assertEquals(rewardsEarned.add(amountReturnedToCreator), totalReward);
  }
}
