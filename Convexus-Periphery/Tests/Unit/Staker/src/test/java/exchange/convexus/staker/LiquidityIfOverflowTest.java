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
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import com.iconloop.score.test.ServiceManager;

import static exchange.convexus.utils.IntUtils.MAX_UINT96;
import static exchange.convexus.utils.SleepUtils.sleep;
import static exchange.convexus.utils.TimeUtils.now;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.test.nft.NFTUtils;
import exchange.convexus.periphery.staker.IncentiveId;
import exchange.convexus.test.staker.ConvexusStakerUtils;

public class LiquidityIfOverflowTest extends ConvexusStakerTest {
  
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
    
    timestamps = makeTimestamps(now().add(BigInteger.valueOf(1000)));
    
    ConvexusStakerUtils.createIncentive(incentiveCreator, rwtk.score, totalReward, staker.getAddress(), pool1.getAddress(), timestamps[0], timestamps[1]);
    incentiveKey = new IncentiveKey(
      rwtk.getAddress(), 
      pool1.getAddress(), 
      timestamps[0], 
      timestamps[1], 
      incentiveCreator.getAddress()
    );
    incentiveId = IncentiveId.compute(incentiveKey);

    sleep(timestamps[0].add(BigInteger.ONE).subtract(now()));
  }

  @Test
  void testWorksWhenNoOverflow () {
    // With this `amount`, liquidity ends up less than MAX_UINT96
    var amount = MAX_UINT96.divide(BigInteger.valueOf(1000));
    
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), sicx.score, amount);
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), usdc.score, amount);

    tokenId = NFTUtils.mintPosition (
      nft,
      lpUser0, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      0,
      TICK_SPACINGS[MEDIUM] * 10,
      amount,
      amount,
      ZERO, 
      ZERO, 
      lpUser0.getAddress(),
      now().add(BigInteger.valueOf(1000))
    );

    NFTUtils.safeTransferFrom(nft, lpUser0, staker.getAddress(), tokenId);
    ConvexusStakerUtils.stakeToken(staker, lpUser0, incentiveKey, tokenId);
    
    var stakes = StakesResult.fromMap(staker.call("stakes", tokenId, incentiveId));
    assertTrue(stakes.liquidity.compareTo(MAX_UINT96) < 0);
  }

  @Test
  void testWorksWhenOverflow () {
    // With this `amount`, liquidity ends up more than MAX_UINT96
    var amount = MAX_UINT96.subtract(BigInteger.valueOf(100));
    
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), sicx.score, amount);
    ConvexusLiquidityUtils.deposit(lpUser0, nft.getAddress(), usdc.score, amount);

    tokenId = NFTUtils.mintPosition (
      nft,
      lpUser0, 
      sicx.getAddress(), 
      usdc.getAddress(), 
      FEE_AMOUNTS[MEDIUM], 
      0,
      TICK_SPACINGS[MEDIUM] * 10,
      amount,
      amount,
      ZERO, 
      ZERO, 
      lpUser0.getAddress(),
      now().add(BigInteger.valueOf(1000))
    );

    NFTUtils.safeTransferFrom(nft, lpUser0, staker.getAddress(), tokenId);
    ConvexusStakerUtils.stakeToken(staker, lpUser0, incentiveKey, tokenId);
    
    var stakes = StakesResult.fromMap(staker.call("stakes", tokenId, incentiveId));
    assertTrue(stakes.liquidity.compareTo(MAX_UINT96) > 0);
  }
}
