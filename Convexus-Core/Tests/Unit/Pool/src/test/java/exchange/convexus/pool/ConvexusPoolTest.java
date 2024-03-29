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

package exchange.convexus.pool;

import exchange.convexus.utils.ICX;
import exchange.convexus.utils.IntUtils;
import score.Address;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import org.mockito.ArgumentCaptor;
import exchange.convexus.core.pool.contracts.models.Positions;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.mocks.factory.ConvexusFactoryMock;
import exchange.convexus.test.liquidity.ConvexusLiquidityUtils;
import exchange.convexus.mocks.pool.ConvexusPoolMock;
import exchange.convexus.test.tokens.Sicx;
import exchange.convexus.test.tokens.Usdc;
import exchange.convexus.utils.ScoreSpy;
import exchange.convexus.test.ConvexusTest;
import exchange.convexus.test.contracts.callee.ConvexusPoolCallee;
import exchange.convexus.test.contracts.reentrantcallee.ConvexusReentrantCallee;
import exchange.convexus.test.contracts.swappay.ConvexusSwapPay;

public class ConvexusPoolTest extends ConvexusTest {

  ScoreSpy<ConvexusPoolMock> pool;
  ScoreSpy<ConvexusFactoryMock> factory;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<ConvexusPoolCallee> callee;
  ScoreSpy<ConvexusReentrantCallee> reentrantCallee;
  ScoreSpy<ConvexusSwapPay> underpay;

  void setup_tokens () throws Exception {
    ICX.ADDRESS = Address.fromString("cx0000000000000000000000000000000000000001");
    sicx = deploy_sicx();
    usdc = deploy_usdc();
    
    // Transfer some funds to Alice
    sicx.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    // Transfer some funds to Bob
    sicx.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
  }

  void setup_pool (Address factory, int fee, int tickSpacing) throws Exception {
    pool = deploy_mock_pool(sicx.getAddress(), usdc.getAddress(), factory, fee, tickSpacing);
    callee = deploy_callee();
    reentrantCallee = deploy_reentrant_callee();
    underpay = deploy_swap_pay();
  }

  void setup_pool_icx (Address factory, int fee, int tickSpacing) throws Exception {
    pool = deploy_mock_pool(ICX.getAddress(), usdc.getAddress(), factory, fee, tickSpacing);
    callee = deploy_callee();
    reentrantCallee = deploy_reentrant_callee();
    underpay = deploy_swap_pay();
  }

  void setup_factory () throws Exception {
    factory = deploy_factory();
  }

  protected void assertObservationEquals(Oracle.Observation expected, Oracle.Observation actual) {
    assertEquals(expected.initialized, actual.initialized);
    assertEquals(expected.tickCumulative, actual.tickCumulative);
    assertEquals(expected.secondsPerLiquidityCumulativeX128, actual.secondsPerLiquidityCumulativeX128);
  }

  protected void callSwap (
    Account from, 
    String method, 
    Address pool, 
    Address callee, 
    Score token, 
    BigInteger _value, 
    Address recipient, 
    BigInteger sqrtPriceLimitX96
  ) {
    var params = Json.object()
      .add("pool", pool.toString())
      .add("recipient", recipient.toString())
      .add("sqrtPriceLimitX96", sqrtPriceLimitX96.toString());

    JsonObject data = Json.object()
      .add("method", method)
      .add("params", params);

    byte[] dataBytes = data.toString().getBytes();

    token.invoke(
      from, 
      "transfer", 
      callee, 
      _value, 
      dataBytes
    );
  }

  protected void callSwapIcx (
    Account from, 
    String method, 
    Address pool, 
    ScoreSpy<?> callee, 
    BigInteger value, 
    Address recipient, 
    BigInteger sqrtPriceLimitX96
  ) {
    callee.invoke(from, value, method + "Icx", pool, recipient, sqrtPriceLimitX96);
  }

  protected void swap (
    Account from,
    Score inputToken,
    BigInteger amountIn,
    BigInteger amountOut,
    BigInteger sqrtPriceLimitX96
  ) {
    boolean exactInput = amountOut.equals(ZERO);

    String method = 
      inputToken.equals(sicx.score)
        ? exactInput
          ? "swapExact0For1"
          : "swap0ForExact1"
        : exactInput
        ? "swapExact1For0"
        : "swap1ForExact0";
      
    if (sqrtPriceLimitX96 == null) {
      if (inputToken.equals(sicx.score)) {
        sqrtPriceLimitX96 = TickMath.MIN_SQRT_RATIO.add(ONE);
      } else {
        sqrtPriceLimitX96 = TickMath.MAX_SQRT_RATIO.subtract(ONE);
      }
    }

    callSwap (
      from,
      method,
      pool.getAddress(), 
      callee.getAddress(), 
      inputToken, 
      exactInput ? amountIn : amountOut, 
      from.getAddress(), 
      sqrtPriceLimitX96
    );
  }
  

  protected void swapIcx (
    Account from,
    BigInteger amountIn,
    BigInteger amountOut,
    BigInteger sqrtPriceLimitX96
  ) {
    boolean exactInput = amountOut.equals(ZERO);

    String method = exactInput
          ? "swapExact0For1"
          : "swap0ForExact1";
      
    if (sqrtPriceLimitX96 == null) {
      sqrtPriceLimitX96 = TickMath.MIN_SQRT_RATIO.add(ONE);
    }

    callSwapIcx (
      from,
      method,
      pool.getAddress(), 
      callee, 
      exactInput ? amountIn : amountOut, 
      from.getAddress(), 
      sqrtPriceLimitX96
    );
  }
  
  protected void swapExact0For1 (BigInteger amount, Account caller, BigInteger sqrtPriceLimitX96) {
    swap(caller, sicx.score, amount, ZERO, sqrtPriceLimitX96);
  }
  protected void swapExact0For1 (BigInteger amount, Account caller) {
    swap(caller, sicx.score, amount, ZERO, null);
  }

  protected void swap0ForExact1 (BigInteger amount, Account caller, BigInteger sqrtPriceLimitX96) {
    swap(caller, sicx.score, ZERO, amount, sqrtPriceLimitX96);
  }
  protected void swap0ForExact1 (BigInteger amount, Account caller) {
    swap(caller, sicx.score, ZERO, amount, null);
  }

  protected void swapExact0For1Icx (BigInteger amount, Account caller, BigInteger sqrtPriceLimitX96) {
    swapIcx(caller, amount, ZERO, sqrtPriceLimitX96);
  }
  protected void swapExact0For1Icx (BigInteger amount, Account caller) {
    swapIcx(caller, amount, ZERO, null);
  }

  protected void swap0ForExact1Icx (BigInteger amount, Account caller, BigInteger sqrtPriceLimitX96) {
    swapIcx(caller, ZERO, amount, sqrtPriceLimitX96);
  }
  protected void swap0ForExact1Icx (BigInteger amount, Account caller) {
    swapIcx(caller, ZERO, amount, null);
  }

  protected void swapExact1For0 (BigInteger amount, Account caller, BigInteger sqrtPriceLimitX96) {
    swap(caller, usdc.score, amount, ZERO, sqrtPriceLimitX96);
  }
  protected void swapExact1For0 (BigInteger amount, Account caller) {
    swap(caller, usdc.score, amount, ZERO, null);
  }

  protected void swap1ForExact0 (BigInteger amount, Account caller, BigInteger sqrtPriceLimitX96) {
    swap(caller, usdc.score, ZERO, amount, sqrtPriceLimitX96);
  }
  protected void swap1ForExact0 (BigInteger amount, Account caller) {
    swap(caller, usdc.score, ZERO, amount, null);
  }

  protected void swapToLowerPrice(Account caller, BigInteger sqrtPriceX96, String tokenAmount) {
    swapToSqrtPrice(caller, sicx.score, sqrtPriceX96, caller.getAddress(), new BigInteger(tokenAmount));
  }

  protected void swapToHigherPrice(Account caller, BigInteger sqrtPriceX96, String tokenAmount) {
    swapToSqrtPrice(caller, usdc.score, sqrtPriceX96, caller.getAddress(), new BigInteger(tokenAmount));
  }

  protected void callSwapToSqrtPrice (
    Account from, 
    String method, 
    Address pool, 
    Address callee, 
    Score token, 
    BigInteger _value, 
    Address recipient, 
    BigInteger sqrtPriceX96
  ) {
    var params = Json.object()
      .add("pool", pool.toString())
      .add("recipient", recipient.toString())
      .add("sqrtPriceX96", sqrtPriceX96.toString());

    JsonObject data = Json.object()
      .add("method", method)
      .add("params", params);

    byte[] dataBytes = data.toString().getBytes();

    token.invoke(
      from, 
      "transfer", 
      callee, 
      _value, 
      dataBytes
    );
  }

  private void swapToSqrtPrice(Account caller, Score tokenScore, BigInteger targetPrice, Address to, BigInteger tokenAmount) {
    if (tokenScore.getAddress().equals(sicx.getAddress())) {
      callSwapToSqrtPrice(caller, "swapToLowerSqrtPrice", pool.getAddress(), callee.getAddress(), tokenScore, tokenAmount, to, targetPrice);
    } else {
      callSwapToSqrtPrice(caller, "swapToHigherSqrtPrice", pool.getAddress(), callee.getAddress(), tokenScore, tokenAmount, to, targetPrice);
    }
  }

  protected void initializeAtZeroTick() {
    BigInteger initializeLiquidityAmount = BigInteger.TEN.pow(18).multiply(TWO);
    pool.invoke(alice, "initialize", encodePriceSqrt(ONE, ONE));
    int tickSpacing = ((BigInteger) pool.call("tickSpacing")).intValue();
    int min = getMinTick(tickSpacing);
    int max = getMaxTick(tickSpacing);
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), usdc.score, new BigInteger("2000000000000000000"));
    ConvexusLiquidityUtils.deposit(alice, callee.getAddress(), sicx.score, new BigInteger("2000000000000000000"));
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
      ConvexusLiquidityUtils.deposit(account, callee.getAddress(), sicx.score, sicxAmount);
    }
    
    if (usdcAmount.compareTo(ZERO) > 0) {
      ConvexusLiquidityUtils.deposit(account, callee.getAddress(), usdc.score, usdcAmount);
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
    return Position.Info.fromMap(pool.call("positions", Positions.getKey(account.getAddress(), minTick, maxTick)));
  }

  protected void setFeeGrowthGlobal0X128(BigInteger _feeGrowthGlobal0X128) {
    pool.invoke(owner, "setFeeGrowthGlobal0X128", _feeGrowthGlobal0X128);
  }

  protected void setFeeGrowthGlobal1X128(BigInteger _feeGrowthGlobal1X128) {
    pool.invoke(owner, "setFeeGrowthGlobal1X128", _feeGrowthGlobal1X128);
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
    boolean zeroForOne,
    boolean poke
  ) {
    if (zeroForOne) {
      swapExact0For1(amount, alice); 
    }
    else {
      swapExact1For0(amount, alice);
    }

    if (poke) {
      burn(minTick, maxTick, ZERO);
    }
  }

  protected Fees swapAndGetFeesOwed (
    int minTick, int maxTick,
    BigInteger amount,
    boolean zeroForOne,
    boolean poke
  ) {
    doSwap(minTick, maxTick, amount, zeroForOne, poke);
    return collectGetFeesOwed(minTick, maxTick);
  }
  
  protected void flash(Account from, BigInteger amount0, BigInteger amount1, Address recipient, BigInteger sicxAmount, BigInteger usdcAmount) {
    if (sicxAmount.compareTo(ZERO) > 0) {
      ConvexusLiquidityUtils.deposit(from, callee.getAddress(), sicx.score, sicxAmount);
    }
    if (usdcAmount.compareTo(ZERO) > 0) {
      ConvexusLiquidityUtils.deposit(from, callee.getAddress(), usdc.score, usdcAmount);
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
      ConvexusLiquidityUtils.deposit(recipient, underpay.getAddress(), sicx.score, pay0);
    }
    if (pay1.compareTo(ZERO) > 0) {
      ConvexusLiquidityUtils.deposit(recipient, underpay.getAddress(), usdc.score, pay1);
    }

    underpay.invoke(recipient, "swap", pool.getAddress(), recipient.getAddress(), zeroForOne, sqrtPriceX96, amountSpecified, pay0, pay1);
  }
  
  protected BigInteger getMaxLiquidityPerTick(int tickSpacing) {
    return TWO.pow(128).subtract(ONE).divide(
      BigInteger.valueOf((getMaxTick(tickSpacing) - getMinTick(tickSpacing)) / tickSpacing + 1)
    );
  }
}
