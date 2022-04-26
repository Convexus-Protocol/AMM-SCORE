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

package exchange.convexus.librairies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.periphery.librairies.LiquidityAmounts;
import exchange.convexus.utils.ConvexusTest;

public class LiquidityForAmountTest extends ConvexusTest {
  
  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
  }

  @Test
  void testAmountsForPriceInside () {
    var sqrtPriceX96 = encodePriceSqrt(1, 1);
    var sqrtPriceAX96 = encodePriceSqrt(100, 110);
    var sqrtPriceBX96 = encodePriceSqrt(110, 100);
    var liquidity = LiquidityAmounts.getLiquidityForAmounts(sqrtPriceX96, sqrtPriceAX96, sqrtPriceBX96, BigInteger.valueOf(100), BigInteger.valueOf(200));
    assertEquals(BigInteger.valueOf(2148), liquidity);
  }
  
  @Test
  void testAmountsForPriceBelow () {
    var sqrtPriceX96 = encodePriceSqrt(99, 110);
    var sqrtPriceAX96 = encodePriceSqrt(100, 110);
    var sqrtPriceBX96 = encodePriceSqrt(110, 100);
    var liquidity = LiquidityAmounts.getLiquidityForAmounts(sqrtPriceX96, sqrtPriceAX96, sqrtPriceBX96, BigInteger.valueOf(100), BigInteger.valueOf(200));
    assertEquals(BigInteger.valueOf(1048), liquidity);
  }
  
  @Test
  void testAmountsForPriceAbove () {
    var sqrtPriceX96 = encodePriceSqrt(110, 100);
    var sqrtPriceAX96 = encodePriceSqrt(100, 110);
    var sqrtPriceBX96 = encodePriceSqrt(110, 100);
    var liquidity = LiquidityAmounts.getLiquidityForAmounts(sqrtPriceX96, sqrtPriceAX96, sqrtPriceBX96, BigInteger.valueOf(100), BigInteger.valueOf(200));
    assertEquals(BigInteger.valueOf(2097), liquidity);
  }
  
  @Test
  void testAmountsForPriceEqualToLowerBoundary () {
    var sqrtPriceAX96 = encodePriceSqrt(100, 110);
    var sqrtPriceX96 = sqrtPriceAX96;
    var sqrtPriceBX96 = encodePriceSqrt(110, 100);
    var liquidity = LiquidityAmounts.getLiquidityForAmounts(sqrtPriceX96, sqrtPriceAX96, sqrtPriceBX96, BigInteger.valueOf(100), BigInteger.valueOf(200));
    assertEquals(BigInteger.valueOf(1048), liquidity);
  }
  
  @Test
  void testAmountsForPriceEqualToUpperBoundary () {
    var sqrtPriceAX96 = encodePriceSqrt(100, 110);
    var sqrtPriceBX96 = encodePriceSqrt(110, 100);
    var sqrtPriceX96 = sqrtPriceBX96;
    var liquidity = LiquidityAmounts.getLiquidityForAmounts(sqrtPriceX96, sqrtPriceAX96, sqrtPriceBX96, BigInteger.valueOf(100), BigInteger.valueOf(200));
    assertEquals(BigInteger.valueOf(2097), liquidity);
  }
}
