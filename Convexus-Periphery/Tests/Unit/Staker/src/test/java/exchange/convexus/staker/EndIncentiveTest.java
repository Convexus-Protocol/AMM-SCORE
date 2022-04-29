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

import com.iconloop.score.test.ServiceManager;

import static exchange.convexus.utils.SleepUtils.sleepTo;
import static exchange.convexus.utils.TimeUtils.now;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.periphery.staker.IncentiveId;
import exchange.convexus.utils.AssertUtils;
import score.Address;
import exchange.convexus.test.staker.ConvexusStakerUtils;

public class EndIncentiveTest extends ConvexusStakerTest {

  BigInteger[] timestamps;
  BigInteger tokenId;
  final BigInteger totalReward = EXA.multiply(BigInteger.valueOf(100));
  final BigInteger amountDesired = EXA.multiply(TEN);
  BigInteger claimable = ZERO;
  IncentiveKey incentiveKey;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_staker();
    setup_tokens();
    setup_pool1();
    setup_pool2();
    
    timestamps = makeTimestamps(now());

    incentiveKey = new IncentiveKey(rwtk.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1], incentiveCreator.getAddress());
    ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, staker.getAddress(), incentiveKey, totalReward);
  }

  void subject () {
    subject(timestamps);
  }

  void subject (BigInteger[] timestamps) {
    staker.invoke(incentiveCreator, "endIncentive", new IncentiveKey(rwtk.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1], incentiveCreator.getAddress()));
  }

  @Test
  void testEmitsIncentiveEndedEvent () {
    sleepTo(timestamps[1].add(BigInteger.TEN));

    byte[] incentiveId = IncentiveId.compute(incentiveKey);

    reset(staker.spy);
    subject();
    verify(staker.spy).IncentiveEnded(incentiveId, totalReward);
  }

  @Test
  void testDeletesIncentivesKey () {
    byte[] incentiveId = IncentiveId.compute(incentiveKey);

    var incentive = Incentive.fromMap(staker.call("incentives", incentiveId));
    assertTrue(incentive.totalRewardUnclaimed.compareTo(ZERO) > 0);
    
    sleepTo(timestamps[1].add(ONE));
    subject();
    
    incentive = Incentive.fromMap(staker.call("incentives", incentiveId));
    assertEquals(ZERO, incentive.totalRewardUnclaimed);
    assertEquals(ZERO, incentive.totalSecondsClaimedX128);
    assertEquals(ZERO, incentive.numberOfStakes);
  }

  @Test
  void testBlockTimestampLteEndTime () {
    sleepTo(timestamps[1].subtract(TEN));

    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> subject(), 
      "endIncentive: cannot end incentive before end time");
  }

  @Test
  void testIncentiveDoesNotExist () {
    // Adjust the block.timestamp so it is after the claim deadline
    sleepTo(timestamps[1].add(ONE));
    timestamps[0] = now().add(BigInteger.valueOf(1000));
    
    AssertUtils.assertThrowsMessage(AssertionError.class, 
      () -> subject(timestamps), 
      "endIncentive: no refund available");
  }

  @Test
  void testIncentiveHasStakes () {
    sleepTo(timestamps[0]);
    BigInteger amountDesired = EXA.multiply(TEN);

    // stake a token
    mintDepositStake(
      lpUser0, 
      new Address[] {sicx.getAddress(), usdc.getAddress()},
      new BigInteger[] {amountDesired, amountDesired}, 
      new int[] {getMinTick(TICK_SPACINGS[MEDIUM]), getMaxTick(TICK_SPACINGS[MEDIUM])},
      timestamps[0], timestamps[1],
      pool1.getAddress(),
      incentiveCreator.getAddress()
    );

    // Adjust the block.timestamp so it is after the claim deadline
    sleepTo(timestamps[1].add(ONE));
    AssertUtils.assertThrowsMessage(AssertionError.class,
      () -> subject(), 
      "endIncentive: cannot end incentive while deposits are staked");
  }
}
