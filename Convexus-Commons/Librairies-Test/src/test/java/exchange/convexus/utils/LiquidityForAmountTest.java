package exchange.convexus.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.librairies.LiquidityAmounts;

public class LiquidityForAmountTest extends UtilsTest {
  
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
