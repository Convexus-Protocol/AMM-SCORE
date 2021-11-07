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

package exchange.convexus.pool;

import exchange.convexus.utils.ConvexusTest;
import exchange.convexus.utils.IntUtils;
import score.Address;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import exchange.convexus.callee.ConvexusCallee;
import exchange.convexus.factory.ConvexusFactory;
import exchange.convexus.librairies.Position;
import exchange.convexus.librairies.Positions;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.librairies.Oracle.Observation;
import exchange.convexus.liquidity.ConvexusLiquidity;
import exchange.convexus.reentrantcallee.ConvexusReentrantCallee;
import exchange.convexus.swappay.ConvexusSwapPay;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;
import exchange.convexus.utils.ScoreSpy;

public class ConvexusPoolTest extends ConvexusTest {

  ScoreSpy<ConvexusPoolMock> pool;
  ScoreSpy<ConvexusFactory> factory;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<ConvexusCallee> callee;
  ScoreSpy<ConvexusReentrantCallee> reentrantCallee;
  ScoreSpy<ConvexusSwapPay> underpay;

  void setup_tokens () throws Exception {
    sicx = deploy_sicx();
    usdc = deploy_usdc();
    
    // Transfer some funds to Alice
    sicx.invoke(owner, "mintTo", alice.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));
    usdc.invoke(owner, "mintTo", alice.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));
    // Transfer some funds to Bob
    sicx.invoke(owner, "mintTo", bob.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));
    usdc.invoke(owner, "mintTo", bob.getAddress(), TEN.pow(30).multiply(TEN.pow(18)));

  }

  void setup_pool (Address factory, int fee, int tickSpacing) throws Exception {
    pool = deploy_mock_pool(sicx.getAddress(), usdc.getAddress(), factory, fee, tickSpacing);
    callee = deploy_callee();
    reentrantCallee = deploy_reentrant_callee();
    underpay = deploy_swap_pay();
  }

  void setup_factory () throws Exception {
    factory = deploy_factory();
  }

  public ScoreSpy<ConvexusPoolMock> deploy_mock_pool (Address token0, Address token1, Address factory, int fee, int tickSpacing) throws Exception {
    Score score = sm.deploy(owner, ConvexusPoolMock.class, token0, token1, factory, fee, tickSpacing);

    var spy = (ConvexusPoolMock) Mockito.spy(score.getInstance());
    score.setInstance(spy);
    return new ScoreSpy<ConvexusPoolMock>(score, spy);
  }

  protected BigInteger encodePriceSqrt (BigInteger reserve1, BigInteger reserve0) {
    return new BigDecimal(reserve1).divide(new BigDecimal(reserve0), MathContext.DECIMAL128).sqrt(MathContext.DECIMAL128).multiply(BigDecimal.valueOf(2).pow(96)).toBigInteger();
  }

  protected int getMinTick(int tickSpacing) {
    return ((int) Math.ceil(-887272 / tickSpacing)) * tickSpacing;
  }

  protected int getMaxTick(int tickSpacing) {
    return ((int) Math.floor(887272 / tickSpacing)) * tickSpacing;
  }

  protected void assertObservationEquals(Observation expected, Observation actual) {
    assertEquals(expected.initialized, actual.initialized);
    assertEquals(expected.tickCumulative, actual.tickCumulative);
    assertEquals(expected.secondsPerLiquidityCumulativeX128, actual.secondsPerLiquidityCumulativeX128);
  }

  protected void swapExact0For1 (BigInteger amount, Account caller) {
    BigInteger sqrtPriceLimitX96 = TickMath.MIN_SQRT_RATIO.add(ONE);
    callee.invoke(caller, "swapExact0For1", pool.getAddress(), amount, caller.getAddress(), sqrtPriceLimitX96);
  }

  protected void swapExact0For1 (BigInteger amount, Account caller, BigInteger sicxAmount) {
    ConvexusLiquidity.deposit(caller, callee.getAddress(), sicx.score, sicxAmount);
    BigInteger sqrtPriceLimitX96 = TickMath.MIN_SQRT_RATIO.add(ONE);
    callee.invoke(caller, "swapExact0For1", pool.getAddress(), amount, caller.getAddress(), sqrtPriceLimitX96);
  }

  protected void swapExact0For1 (BigInteger amount, Account caller, String sicxAmount) {
    swapExact0For1(amount, caller, new BigInteger(sicxAmount));
  }

  protected void swapExact1For0(BigInteger amount, Account caller) {
    BigInteger sqrtPriceLimitX96 = TickMath.MAX_SQRT_RATIO.subtract(ONE);
    callee.invoke(caller, "swapExact1For0", pool.getAddress(), amount, caller.getAddress(), sqrtPriceLimitX96);
  }

  protected void swapExact1For0 (BigInteger amount, Account caller, BigInteger usdcAmount) {
    ConvexusLiquidity.deposit(caller, callee.getAddress(), usdc.score, usdcAmount);
    BigInteger sqrtPriceLimitX96 = TickMath.MAX_SQRT_RATIO.subtract(ONE);
    callee.invoke(caller, "swapExact1For0", pool.getAddress(), amount, caller.getAddress(), sqrtPriceLimitX96);
  }

  protected void swapExact1For0 (BigInteger amount, Account caller, String usdcAmount) {
    swapExact1For0(amount, caller, new BigInteger(usdcAmount));
  }

  protected void swapToLowerPrice(Account caller, BigInteger sqrtPriceX96, Account to, String tokenAmount) {
    swapToSqrtPrice(caller, sicx.score, sqrtPriceX96, to.getAddress(), new BigInteger(tokenAmount));
  }

  protected void swapToHigherPrice(Account caller, BigInteger sqrtPriceX96, Account to, String tokenAmount) {
    swapToSqrtPrice(caller, usdc.score, sqrtPriceX96, to.getAddress(), new BigInteger(tokenAmount));
  }

  private void swapToSqrtPrice(Account caller, Score tokenScore, BigInteger targetPrice, Address to, BigInteger tokenAmount) {
    if (tokenAmount.compareTo(ZERO) > 0) {
      ConvexusLiquidity.deposit(caller, callee.getAddress(), tokenScore, tokenAmount);
    }
    if (tokenScore.getAddress().equals(sicx.getAddress())) {
      callee.invoke(caller, "swapToLowerSqrtPrice", pool.getAddress(), targetPrice, to);
    } else {
      callee.invoke(caller, "swapToHigherSqrtPrice", pool.getAddress(), targetPrice, to);
    }
  }

  protected void initializeAtZeroTick() {
    BigInteger initializeLiquidityAmount = BigInteger.TEN.pow(18).multiply(BigInteger.TWO);
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    int tickSpacing = ((BigInteger) pool.call("tickSpacing")).intValue();
    int min = getMinTick(tickSpacing);
    int max = getMaxTick(tickSpacing);
    ConvexusLiquidity.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("2000000000000000000"));
    ConvexusLiquidity.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("2000000000000000000"));
    callee.invoke(alice, "mint", pool.getAddress(), alice.getAddress(), min, max, initializeLiquidityAmount);
  }

  protected BigInteger expandTo18Decimals(int i) {
    return BigInteger.TEN.pow(18).multiply(BigInteger.valueOf(i));
  }
  
  protected void mint(Account account, int minTick, int maxTick, BigInteger amount, String sicxAmount, String usdcAmount) {
    mint(account, minTick, maxTick, amount, new BigInteger(sicxAmount), new BigInteger(usdcAmount));
  }

  protected void mint(Account account, int minTick, int maxTick, BigInteger amount, BigInteger sicxAmount, BigInteger usdcAmount) {
    if (sicxAmount.compareTo(ZERO) > 0) {
      ConvexusLiquidity.deposit(account, callee.getAddress(), sicx.score, sicxAmount);
    }
    
    if (usdcAmount.compareTo(ZERO) > 0) {
      ConvexusLiquidity.deposit(account, callee.getAddress(), usdc.score, usdcAmount);
    }

    callee.invoke(account, "mint", pool.getAddress(), account.getAddress(), minTick, maxTick, amount);
  }

  protected void burn (int minTick, int maxTick, BigInteger amount) {
    burn(alice, minTick, maxTick, amount);
  }
  
  protected void burn (Account from, int minTick, int maxTick, BigInteger amount) {
    pool.invoke(from, "burn", minTick, maxTick, amount);
  }
  
  protected Position.Info positions (Account account, int minTick, int maxTick) {
    return (Position.Info) pool.call("positions", Positions.getKey(account.getAddress(), minTick, maxTick));
  }

  protected void setFeeGrowthGlobal0X128(BigInteger _feeGrowthGlobal0X128) {
    pool.invoke(owner, "setFeeGrowthGlobal0X128", _feeGrowthGlobal0X128);
  }

  protected void setFeeGrowthGlobal1X128(BigInteger _feeGrowthGlobal1X128) {
    pool.invoke(owner, "setFeeGrowthGlobal1X128", _feeGrowthGlobal1X128);
  }
  
  class Fees {
    public Fees(BigInteger token0Fees, BigInteger token1Fees) {
      this.token0Fees = token0Fees;
      this.token1Fees = token1Fees;
    }
    public BigInteger token0Fees;
    public BigInteger token1Fees;
  }
  
  protected Fees collectGetFeesOwed (int minTick, int maxTick) {
    return collectGetFeesOwed(alice, minTick, maxTick);
  }
  
  protected Fees collectGetFeesOwed (Account from, int minTick, int maxTick) {
    reset(pool.spy);
    pool.invoke(from, "collect", from.getAddress(), minTick, maxTick, IntUtils.MAX_UINT128, IntUtils.MAX_UINT128);

    // Get Collect event
    ArgumentCaptor<Address> _caller = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _tickLower = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickUpper = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(pool.spy).Collect(_caller.capture(), _tickLower.capture(), _tickUpper.capture(), _recipient.capture(), _amount0.capture(), _amount1.capture());
    
    assertTrue(_amount0.getValue().compareTo(ZERO) >= 0);
    assertTrue(_amount1.getValue().compareTo(ZERO) >= 0);

    return new Fees(_amount0.getValue(), _amount1.getValue());
  }

  protected Fees collectProtocolGetFeesOwed (Account from, Account to) {
    reset(pool.spy);
    pool.invoke(from, "collectProtocol", to.getAddress(), IntUtils.MAX_UINT128, IntUtils.MAX_UINT128);

    // Get CollectProtocol event
    ArgumentCaptor<Address> _caller = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0 = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1 = ArgumentCaptor.forClass(BigInteger.class);
    verify(pool.spy).CollectProtocol(_caller.capture(), _recipient.capture(), _amount0.capture(), _amount1.capture());
    
    return new Fees(_amount0.getValue(), _amount1.getValue());
  }

  protected void doSwap (
    int minTick, int maxTick,
    BigInteger amount,
    String tokenAmount,
    boolean zeroForOne,
    boolean poke
  ) {
    if (zeroForOne) {
      swapExact0For1(amount, alice, tokenAmount); 
    }
    else {
      swapExact1For0(amount, alice, tokenAmount);
    }

    if (poke) {
      burn(minTick, maxTick, ZERO);
    }
  }

  protected Fees swapAndGetFeesOwed (
    int minTick, int maxTick,
    BigInteger amount,
    String tokenAmount,
    boolean zeroForOne,
    boolean poke
  ) {
    doSwap(minTick, maxTick, amount, tokenAmount, zeroForOne, poke);
    return collectGetFeesOwed(minTick, maxTick);
  }
  
  protected void flash(Account from, BigInteger amount0, BigInteger amount1, Address recipient, BigInteger sicxAmount, BigInteger usdcAmount) {
    if (sicxAmount.compareTo(ZERO) > 0) {
      ConvexusLiquidity.deposit(from, callee.getAddress(), sicx.score, sicxAmount);
    }
    if (usdcAmount.compareTo(ZERO) > 0) {
      ConvexusLiquidity.deposit(from, callee.getAddress(), usdc.score, usdcAmount);
    }

    callee.invoke(from, "flash", pool.getAddress(), recipient, amount0, amount1, sicxAmount, usdcAmount);
  }
  
  protected void flash(Account from, BigInteger amount0, BigInteger amount1, Account recipient, BigInteger sicxAmount, BigInteger usdcAmount) {
    flash(from, amount0, amount1, recipient.getAddress(), sicxAmount, usdcAmount);
  }
  
  protected void flash(Account from, BigInteger amount0, BigInteger amount1, Account recipient, String sicxAmount, String usdcAmount) {
    flash(from, amount0, amount1, recipient, new BigInteger(sicxAmount), new BigInteger(usdcAmount));
  }

  protected void flash(Account from, String amount0, String amount1, Account recipient, String sicxAmount, String usdcAmount) {
    flash(from, new BigInteger(amount0), new BigInteger(amount1), recipient, new BigInteger(sicxAmount), new BigInteger(usdcAmount));
  }
  
  protected void flash(Account from, String amount0, String amount1, Address recipient, String sicxAmount, String usdcAmount) {
    flash(from, new BigInteger(amount0), new BigInteger(amount1), recipient, new BigInteger(sicxAmount), new BigInteger(usdcAmount));
  }

  protected void underswap_pay (
    Account recipient,
    boolean zeroForOne,
    BigInteger sqrtPriceX96, 
    BigInteger amountSpecified, 
    BigInteger pay0,
    BigInteger pay1
  ) {
    if (pay0.compareTo(ZERO) > 0) {
      ConvexusLiquidity.deposit(recipient, underpay.getAddress(), sicx.score, pay0);
    }
    if (pay1.compareTo(ZERO) > 0) {
      ConvexusLiquidity.deposit(recipient, underpay.getAddress(), usdc.score, pay1);
    }

    underpay.invoke(recipient, "swap", pool.getAddress(), recipient.getAddress(), zeroForOne, sqrtPriceX96, amountSpecified, pay0, pay1);
  }
}
