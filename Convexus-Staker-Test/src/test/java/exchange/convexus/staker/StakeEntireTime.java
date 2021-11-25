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

import static exchange.convexus.utils.SleepUtils.sleep;
import static exchange.convexus.utils.TimeUtils.ONE_DAY;
import static exchange.convexus.utils.TimeUtils.ONE_SECOND;
import static java.math.BigInteger.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static exchange.convexus.utils.TimeUtils.now;

public class StakeEntireTime extends ConvexusStakerTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  BigInteger THOUSAND = BigInteger.valueOf(1000);
  int tickSpacing = TICK_SPACINGS[MEDIUM];
  BigInteger totalReward = EXA.multiply(BigInteger.valueOf(3_000));
  BigInteger duration = ONE_DAY.multiply(BigInteger.valueOf(30));
  BigInteger[] tokensId = new BigInteger[3];
  
  BigInteger startTime;
  BigInteger endTime;

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
    setup_pool2();

    // createIncentive
    ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, totalReward, staker.getAddress(), pool1.getAddress(), startTime, endTime);

    Score[] tokensToStake = {sicx.score, usdc.score};
    sleep(startTime.subtract(now).divide(ONE_SECOND).intValue() + 1);
    tokensId[0] = mintDepositStake(lpUser0, tokensToStake, amountsToStake, ticksToStake, startTime, endTime);
    tokensId[1] = mintDepositStake(lpUser1, tokensToStake, amountsToStake, ticksToStake, startTime, endTime);
    tokensId[2] = mintDepositStake(lpUser2, tokensToStake, amountsToStake, ticksToStake, startTime, endTime);
  }

  @Test
  void testAssumptions () {
    assertEquals(staker.call("factory"), factory.getAddress());
    assertEquals(staker.call("nonfungiblePositionManager"), nft.getAddress());
    assertEquals(staker.call("maxIncentiveDuration"), ONE_SECOND.multiply(TWO.pow(32)));
    assertEquals(staker.call("maxIncentiveStartLeadTime"), ONE_SECOND.multiply(TWO.pow(32)));
  }

  @Test
  void testWhoAllStakeEntireTimeWithdrawAtTheEnd () {
    BigInteger now = now();
    sleep(endTime.subtract(now).divide(ONE_SECOND).intValue() + 1);

    // Sanity check: make sure we go past the incentive end time.
    now = now();
    assertTrue(now.compareTo(endTime) >= 0);

    // Everyone pulls their liquidity at the same time
    unstakeCollectBurnFlow(lpUser0, rwtk.getAddress(), pool1.getAddress(), startTime, endTime, tokensId[0]);
    // TODO: Not Approved ? Why ?
  }
}
