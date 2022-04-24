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
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import static exchange.convexus.utils.SleepUtils.sleep;
import static exchange.convexus.utils.TimeUtils.now;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.nft.NFTUtils;
import exchange.convexus.pool.SnapshotCumulativesInsideResult;
import exchange.convexus.positionmgr.PositionInformation;
import exchange.convexus.utils.MathUtils;
import exchange.convexus.utils.SleepUtils;

public class GetRewardInfoTest extends ConvexusStakerTest {
  
  BigInteger[] timestamps;
  BigInteger tokenId;
  byte[] incentiveId;
  IncentiveKey stakeIncentiveKey;
  final BigInteger totalReward = EXA.multiply(BigInteger.valueOf(100));
  
  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_staker();
    setup_tokens();
    setup_pool1();
    setup_pool2();
    
    timestamps = makeTimestamps(now().add(BigInteger.valueOf(1000)));
    
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), sicx.score, DEFAULT_LP_AMOUNT);
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), usdc.score, DEFAULT_LP_AMOUNT);

    tokenId = NFTUtils.mintPosition(
      nft,
      lpUser0, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      getMinTick(TICK_SPACINGS[MEDIUM]), 
      getMaxTick(TICK_SPACINGS[MEDIUM]), 
      DEFAULT_LP_AMOUNT, 
      DEFAULT_LP_AMOUNT, 
      ZERO, 
      ZERO, 
      lpUser0.getAddress(), 
      now().add(BigInteger.valueOf(1000)));

    NFTUtils.safeTransferFrom(nft, lpUser0, staker.getAddress(), tokenId);

    stakeIncentiveKey = new IncentiveKey(rwtk.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1], incentiveCreator.getAddress());

    incentiveId = IncentiveId.compute(ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, totalReward, staker.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1]));

    SleepUtils.sleep(timestamps[0].subtract(now()));
    ConvexusStakerUtils.stakeToken(staker, lpUser0, stakeIncentiveKey, tokenId);
    staker.call("stakes", tokenId, incentiveId);
  }

  @Test
  void testReturnsCorrectRewardAmountAndSecondsINsideX128ForThePosition () {
    sleep(timestamps[0].subtract(now()).add(BigInteger.valueOf(100)));

    var rewardInfo = RewardAmount.fromMap(staker.call("getRewardInfo", stakeIncentiveKey, tokenId));
    var position = PositionInformation.fromMap(nft.call("positions", tokenId));
    var snapshot = SnapshotCumulativesInsideResult.fromMap(pool1.call("snapshotCumulativesInside", position.tickLower, position.tickUpper));
    var stake = StakesResult.fromMap(staker.call("stakes", tokenId, incentiveId));

    BigInteger expectedSecondsInPeriod = snapshot.secondsPerLiquidityInsideX128.subtract(stake.secondsPerLiquidityInsideInitialX128).multiply(stake.liquidity);

    assertEquals(MathUtils.pow10(19).subtract(ONE), rewardInfo.reward);
    assertEquals(expectedSecondsInPeriod, rewardInfo.secondsInsideX128);
  }

  @Test
  void testReturnsNonZeroForIncentiveAfterEndTime () {
    sleep(timestamps[1].subtract(now()).add(ONE));
    var rewardInfo = RewardAmount.fromMap(staker.call("getRewardInfo", stakeIncentiveKey, tokenId));

    assertNotEquals(ZERO, rewardInfo.reward);
    assertNotEquals(ZERO, rewardInfo.secondsInsideX128);
  }
}
