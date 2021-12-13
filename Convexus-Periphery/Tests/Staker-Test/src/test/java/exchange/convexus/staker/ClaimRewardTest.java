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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import score.Address;

public class ClaimRewardTest extends ConvexusStakerTest {
  
  BigInteger[] timestamps;
  BigInteger tokenId;
  final BigInteger totalReward = EXA.multiply(BigInteger.valueOf(100));
  final BigInteger amountDesired = EXA.multiply(TEN);
  BigInteger claimable = ZERO;

  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_staker();
    setup_tokens();
    setup_pool1();
    setup_pool2();
    
    timestamps = makeTimestamps(now());
    
    ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, totalReward, staker.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1]);

    sleep(timestamps[0].subtract(now()).add(BigInteger.valueOf(1)));
    
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), sicx.score, amountDesired);
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), usdc.score, amountDesired);

    tokenId = mintDepositStake(
      lpUser0, 
      new Address[] {sicx.getAddress(), usdc.getAddress()},
      new BigInteger[] {amountDesired, amountDesired}, 
      new int[] {getMinTick(TICK_SPACINGS[MEDIUM]), getMaxTick(TICK_SPACINGS[MEDIUM])},
      timestamps[0], timestamps[1],
      pool1.getAddress(),
      incentiveCreator.getAddress()
    );

    sleep(timestamps[1].subtract(now()).subtract(ONE));

    unstakeToken(lpUser0,
     new IncentiveKey(rwtk.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1], incentiveCreator.getAddress()), 
     tokenId
    );

    claimable = (BigInteger) staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress());
  }

  void subject (Account from, Address token, Address to, BigInteger amount) {
    staker.invoke(from, "claimReward", token, to, amount);
  }

  // when requesting the full amount
  @Test
  void testFullAmountEmitsRewardClaimedEvent () {
    reset(staker.spy);
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), ZERO);
    verify(staker.spy).RewardClaimed(lpUser0.getAddress(), claimable);
  }
  
  @Test
  void testFullAmountTransfersTheCorrectRewardAmountToDestinationAddress () {
    BigInteger balance = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), ZERO);
    BigInteger after = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    assertEquals(balance.add(claimable), after);
  }

  @Test
  void testFullAmountSetsTheClaimedRewardAmountToZero () {
    assertNotEquals(ZERO, staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress()));
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), ZERO);
    assertEquals(ZERO, staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress()));
  }

  @Test
  void testFullAmountReturnsTheirClaimableAmount () {
    BigInteger before = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), ZERO);
    BigInteger after = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    assertEquals(ZERO, staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress()));
    assertEquals(before.add(claimable), after);
  }

  // when requesting a full amount
  @Test
  void testManualFullAmountEmitsRewardClaimedEvent () {
    reset(staker.spy);
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), claimable);
    verify(staker.spy).RewardClaimed(lpUser0.getAddress(), claimable);
  }

  @Test
  void testManualFullAmountTransfersTheCorrectRewardAmountToDestinationAddress () {
    BigInteger balance = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), claimable);
    BigInteger after = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    assertEquals(balance.add(claimable), after);
  }

  @Test
  void testManualFullAmountSetsTheClaimedRewardAmountToZero () {
    assertNotEquals(ZERO, staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress()));
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), claimable);
    assertEquals(ZERO, staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress()));
  }

  @Test
  void testManualFullAmountReturnsTheirClaimableAmount () {
    BigInteger before = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), claimable);
    BigInteger after = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    assertEquals(ZERO, staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress()));
    assertEquals(before.add(claimable), after);
  }

  // Partial amount
  @Test
  void testPartialAmountEmitsRewardClaimedEvent () {
    reset(staker.spy);
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), claimable.subtract(ONE));
    verify(staker.spy).RewardClaimed(lpUser0.getAddress(), claimable.subtract(ONE));
  }

  @Test
  void testPartialAmountTransfersTheCorrectRewardAmountToDestinationAddress () {
    BigInteger balance = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), claimable.subtract(ONE));
    BigInteger after = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    assertEquals(balance.add(claimable.subtract(ONE)), after);
  }

  @Test
  void testPartialAmountSetsTheClaimedRewardAmountToZero () {
    assertNotEquals(ZERO, staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress()));
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), claimable.subtract(ONE));
    assertEquals(ONE, staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress()));
  }

  @Test
  void testPartialAmountReturnsTheirClaimableAmount () {
    BigInteger before = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    subject(lpUser0, rwtk.getAddress(), lpUser0.getAddress(), claimable.subtract(ONE));
    BigInteger after = (BigInteger) rwtk.call("balanceOf", lpUser0.getAddress());
    assertEquals(ONE, staker.call("rewards", rwtk.getAddress(), lpUser0.getAddress()));
    assertEquals(before.add(claimable.subtract(ONE)), after);
  }
}
