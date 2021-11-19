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
import static java.math.BigInteger.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.MathUtils;
import exchange.convexus.utils.TimeUtils;
import score.Context;

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
  BigInteger duration = TimeUtils.ONE_DAY.multiply(BigInteger.valueOf(30));
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

    final BigInteger now = TimeUtils.nowSeconds();
    BigInteger startTime = now.add(ONE_SECOND.multiply(THOUSAND));
    BigInteger endTime = startTime.add(duration);

    // Create Pool
    setup_pool01();
    setup_pool12();

    // createIncentive
    Context.println("startTime = " + startTime);
    Context.println("endTime = " + endTime);
    ConvexusStakerUtils.createIncentive(alice, rwtk.score, totalReward, staker.getAddress(), pool.getAddress(), startTime, endTime, alice.getAddress());

    Score[] tokensToStake = {sicx.score, usdc.score};
    sm.getBlock().increase(1000 + 1);
    mintDepositStake(lp1, tokensToStake, amountsToStake, ticksToStake);
  }

  @Test
  void testAssumptions () {
    assertEquals(staker.call("factory"), factory.getAddress());
    assertEquals(staker.call("nonfungiblePositionManager"), nonfungiblePositionManager.getAddress());
    assertEquals(staker.call("maxIncentiveDuration"), ONE_SECOND.multiply(TWO.pow(32)));
    assertEquals(staker.call("maxIncentiveStartLeadTime"), ONE_SECOND.multiply(TWO.pow(32)));
  }
}
