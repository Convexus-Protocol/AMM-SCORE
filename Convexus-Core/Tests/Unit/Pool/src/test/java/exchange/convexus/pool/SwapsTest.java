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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.RoundingMode.CEILING;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.utils.AssertUtils;
import exchange.convexus.utils.IntUtils;

public class SwapsTest extends ConvexusPoolTest {

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;

  void before_each() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_factory();
    reset(factory.spy);
  }

  class PoolTestCase {
    String description;
    int feeAmount;
    int tickSpacing;
    BigInteger startingPrice;
    PoolPosition[] positions;
    boolean[] errorsHitmap;

    public PoolTestCase (
      String description,
      int feeAmount,
      int tickSpacing,
      BigInteger startingPrice,
      PoolPosition[] positions,
      boolean[] errorsHitmap
    ) {
        this.description = description;
        this.feeAmount = feeAmount;
        this.tickSpacing = tickSpacing;
        this.startingPrice = startingPrice;
        this.positions = positions;
        this.errorsHitmap = errorsHitmap;
        if (errorsHitmap.length != DEFAULT_POOL_SWAP_TESTS_SIZE) {
          assertTrue(false);
        }
    }
  }

  class PoolPosition {
    int tickLower;
    int tickUpper;
    BigInteger liquidity;
    BigInteger pay0;
    BigInteger pay1;
    public PoolPosition (
      int tickLower,
      int tickUpper,
      BigInteger liquidity
    ) {
      this.tickLower = tickLower;
      this.tickUpper = tickUpper;
      this.liquidity = liquidity;
      this.pay0 = IntUtils.MAX_UINT128;
      this.pay1 = IntUtils.MAX_UINT128;
    }
  }

  class SwapTestCase {
    Boolean zeroForOne;
    Boolean exactOut;
    BigInteger amount0;
    BigInteger amount1;
    BigInteger sqrtPriceLimit;
    BigInteger pay0;
    BigInteger pay1;
    public SwapTestCase () {
      this.zeroForOne = null;
      this.exactOut = null;
      this.amount0 = null;
      this.amount1 = null;
      this.sqrtPriceLimit = null;
      this.pay0 = null;
      this.pay1 = null;
    }
  }

  final SwapTestCase[] DEFAULT_POOL_SWAP_TESTS = {
    // swap large amounts in/out
    new SwapTestCase() {{
      zeroForOne = true;
      exactOut = false;
      amount0 = expandTo18Decimals(1);
    }},
    new SwapTestCase() {{
      zeroForOne = false;
      exactOut = false;
      amount1 = expandTo18Decimals(1);
    }},
    new SwapTestCase() {{
      zeroForOne = true;
      exactOut = true;
      amount1 = expandTo18Decimals(1);
    }},
    new SwapTestCase() {{
      zeroForOne = false;
      exactOut = true;
      amount0 = expandTo18Decimals(1);
    }},
    // swap large amounts in/out with a price limit
    new SwapTestCase() {{
      zeroForOne = true;
      exactOut = false;
      amount0 = expandTo18Decimals(1);
      sqrtPriceLimit = encodePriceSqrt(BigInteger.valueOf(50), BigInteger.valueOf(100));
    }},
    new SwapTestCase() {{
      zeroForOne = false;
      exactOut = false;
      amount1 = expandTo18Decimals(1);
      sqrtPriceLimit = encodePriceSqrt(BigInteger.valueOf(200), BigInteger.valueOf(100));
    }},
    new SwapTestCase() {{
      zeroForOne = true;
      exactOut = true;
      amount1 = expandTo18Decimals(1);
      sqrtPriceLimit = encodePriceSqrt(BigInteger.valueOf(50), BigInteger.valueOf(100));
    }},
    new SwapTestCase() {{
      zeroForOne = false;
      exactOut = true;
      amount0 = expandTo18Decimals(1);
      sqrtPriceLimit = encodePriceSqrt(BigInteger.valueOf(200), BigInteger.valueOf(100));
    }},
    // swap small amounts in/out
    new SwapTestCase() {{
      zeroForOne = true;
      exactOut = false;
      amount0 = BigInteger.valueOf(1000);
    }},
    new SwapTestCase() {{
      zeroForOne = false;
      exactOut = false;
      amount1 = BigInteger.valueOf(1000);
    }},
    new SwapTestCase() {{
      zeroForOne = true;
      exactOut = true;
      amount1 = BigInteger.valueOf(1000);
    }},
    new SwapTestCase() {{
      zeroForOne = false;
      exactOut = true;
      amount0 = BigInteger.valueOf(1000);
    }},
    // swap arbitrary input to price
    new SwapTestCase() {{
      sqrtPriceLimit = encodePriceSqrt(BigInteger.valueOf(5), BigInteger.valueOf(2));
      zeroForOne = false;
    }},
    new SwapTestCase() {{
      sqrtPriceLimit = encodePriceSqrt(BigInteger.valueOf(2), BigInteger.valueOf(5));
      zeroForOne = true;
    }},
    new SwapTestCase() {{
      sqrtPriceLimit = encodePriceSqrt(BigInteger.valueOf(5), BigInteger.valueOf(2));
      zeroForOne = true;
    }},
    new SwapTestCase() {{
      sqrtPriceLimit = encodePriceSqrt(BigInteger.valueOf(2), BigInteger.valueOf(5));
      zeroForOne = false;
    }},
  };
  
  public final int DEFAULT_POOL_SWAP_TESTS_SIZE = DEFAULT_POOL_SWAP_TESTS.length;

  final PoolTestCase[] TEST_POOLS = {
    new PoolTestCase (
      "low fee, 1:1 price, 2e18 max range liquidity",
      FEE_AMOUNTS[LOW],
      TICK_SPACINGS[LOW],
      encodePriceSqrt(BigInteger.valueOf(1), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[LOW]),
          getMaxTick(TICK_SPACINGS[LOW]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, false, false, false, 
        false, false, false, false, 
        false, false, true, true
      }
    ),
    new PoolTestCase (
      "medium fee, 1:1 price, 2e18 max range liquidity",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      encodePriceSqrt(BigInteger.valueOf(1), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, false, false, false, 
        false, false, false, false, 
        false, false, true, true
      }
    ),
    new PoolTestCase (
      "high fee, 1:1 price, 2e18 max range liquidity",
      FEE_AMOUNTS[HIGH],
      TICK_SPACINGS[HIGH],
      encodePriceSqrt(BigInteger.valueOf(1), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[HIGH]),
          getMaxTick(TICK_SPACINGS[HIGH]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, false, false, false, 
        false, false, false, false, 
        false, false, true, true
      }
    ),
    new PoolTestCase (
      "medium fee, 10:1 price, 2e18 max range liquidity",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      encodePriceSqrt(BigInteger.valueOf(10), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, true,  false, true, 
        false, false, false, false, 
        true,  false, false, true
      }
    ),
    new PoolTestCase (
      "medium fee, 1:10 price, 2e18 max range liquidity",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      encodePriceSqrt(BigInteger.valueOf(1), BigInteger.valueOf(10)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        true, false, true, false, 
        false, false, false, false, 
        false, true, true, false
      }
    ),
    new PoolTestCase (
      "medium fee, 1:1 price, 0 liquidity, all liquidity around current price",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      encodePriceSqrt(BigInteger.valueOf(1), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          -TICK_SPACINGS[MEDIUM],
          expandTo18Decimals(2)
        ),
        new PoolPosition(
          TICK_SPACINGS[MEDIUM],
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, false, false, false, 
        false, false, false, false, 
        false, false, true,  true
      }
    ),
    new PoolTestCase (
      "medium fee, 1:1 price, additional liquidity around current price",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      encodePriceSqrt(BigInteger.valueOf(1), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        ),
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          -TICK_SPACINGS[MEDIUM],
          expandTo18Decimals(2)
        ),
        new PoolPosition(
          TICK_SPACINGS[MEDIUM],
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, false, false, false, 
        false, false, false, false, 
        false, false, true,  true
      }
    ),
    new PoolTestCase (
      "low fee, large liquidity around current price / stable swap -",
      FEE_AMOUNTS[LOW],
      TICK_SPACINGS[LOW],
      encodePriceSqrt(BigInteger.valueOf(1), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          -TICK_SPACINGS[LOW],
          TICK_SPACINGS[LOW],
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, false, false, false, 
        false, false, false, false, 
        false, false, true, true
      }
    ),
    new PoolTestCase (
      "medium fee, token0 liquidity only",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      encodePriceSqrt(BigInteger.valueOf(1), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          0,
          2000 * TICK_SPACINGS[MEDIUM],
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, false, false, false, 
        false, false, false, false, 
        false, false, true, true
      }
    ),
    new PoolTestCase (
      "close to max price",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      encodePriceSqrt(TWO.pow(127), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, true,  false, true, 
        false, false, false, false, 
        true,  false, false, true
      }
    ),
    new PoolTestCase (
      "close to max price",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      encodePriceSqrt(BigInteger.valueOf(1), TWO.pow(127)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, false, false, false, 
        true, false, true, false, 
        false, false, false, false, 
        false, true, true, false
      }
    ),
    new PoolTestCase (
      "max full range liquidity at 1:1 price with default fee",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      encodePriceSqrt(BigInteger.valueOf(1), BigInteger.valueOf(1)),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          getMaxLiquidityPerTick(TICK_SPACINGS[MEDIUM])
        )
      },
      new boolean[] {
        false, false, false, false, 
        false, false, false, false, 
        false, false, false, false, 
        false, false, true,  true
      }
    ),
    new PoolTestCase (
      "initialized at the max ratio",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      TickMath.MAX_SQRT_RATIO.subtract(ONE),
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        false, true, false, true, 
        false, true, false, true, 
        false, true, false, true, 
        true, false, false, true
      }
    ),
    new PoolTestCase (
      "initialized at the min ratio",
      FEE_AMOUNTS[MEDIUM],
      TICK_SPACINGS[MEDIUM],
      TickMath.MIN_SQRT_RATIO,
      new PoolPosition[] {
        new PoolPosition(
          getMinTick(TICK_SPACINGS[MEDIUM]),
          getMaxTick(TICK_SPACINGS[MEDIUM]),
          expandTo18Decimals(2)
        )
      },
      new boolean[] {
        true, false, true, false, 
        true, false, true, false, 
        true, false, true, false, 
        false, true, true, false
      }
    )
  };

  private BigInteger[] setup_pool_case (PoolTestCase poolCase) throws Exception {
    before_each();
    setup_pool(factory.getAddress(), poolCase.feeAmount, poolCase.tickSpacing);
    reset(pool.spy);
    ConvexusFactoryUtils.createPool(factory, alice, sicx.getAddress(), usdc.getAddress(), poolCase.feeAmount, pool.getAddress());
    pool.invoke(alice, "initialize", poolCase.startingPrice);

    // mint all positions
    for (var position : poolCase.positions) {
      mint(alice, position.tickLower, position.tickUpper, position.liquidity, position.pay0, position.pay1);
    }

    BigInteger poolBalance0 = (BigInteger) sicx.call("balanceOf", pool.getAddress());
    BigInteger poolBalance1 = (BigInteger) usdc.call("balanceOf", pool.getAddress());

    return new BigInteger[] { poolBalance0, poolBalance1 };
  }

  @TestFactory
  Iterable<DynamicTest> dynamicTestsSwaps() throws Exception {
    List<DynamicTest> tests = new ArrayList<>();

    for (int i = 0; i < TEST_POOLS.length; i++) {
      var poolCase = TEST_POOLS[i];
      final int indexPool = i;

      for (int j = 0 ; j < DEFAULT_POOL_SWAP_TESTS.length; j++) {
        var testCase = DEFAULT_POOL_SWAP_TESTS[j];
        final int indexSwap = j;

        tests.add(DynamicTest.dynamicTest(
          swapCaseToDescription(poolCase, testCase, indexPool, indexSwap),
          () -> { 
            setup_pool_case(poolCase); // var poolBalances = setup_pool_case(poolCase); 

            if (!poolCase.errorsHitmap[indexSwap]) {
              reset(sicx.spy);
              reset(usdc.spy);

              executeSwap(testCase);
              
              // var slot0 = Slot0.fromMap(pool.call("slot0"));
              // BigInteger poolBalance0After = (BigInteger) sicx.call("balanceOf", pool.getAddress());
              // BigInteger poolBalance1After = (BigInteger) usdc.call("balanceOf", pool.getAddress());
              // Slot0 slot0After = Slot0.fromMap(pool.call("slot0"));
              // BigInteger liquidityAfter = (BigInteger) pool.call("liquidity");
              // BigInteger feeGrowthGlobal0X128 = (BigInteger) pool.call("feeGrowthGlobal0X128");
              // BigInteger feeGrowthGlobal1X128 = (BigInteger) pool.call("feeGrowthGlobal1X128");              

              // BigInteger poolBalance0Delta = poolBalance0After.subtract(poolBalances[0]);
              // BigInteger poolBalance1Delta = poolBalance1After.subtract(poolBalances[1]);

            } else {
              AssertUtils.assertThrowsMessage(AssertionError.class, () ->
                executeSwap(testCase), 
                "swap: Wrong sqrtPriceLimitX96"
              );
            }
          }
        ));
      }
    }

    return tests;
  }

  private String swapCaseToDescription(PoolTestCase poolCase, SwapTestCase testCase, int indexPool, int indexSwap) {
    String priceClause = testCase.sqrtPriceLimit != null ? " to price " + formatPrice(testCase.sqrtPriceLimit) : "";
    String description = "[" + String.format("%02d", indexPool) + "] " + "[" + String.format("%02d", indexSwap) + "] " + poolCase.description + " ";

    if (testCase.exactOut != null) {
      if (testCase.exactOut) {
        if (testCase.zeroForOne) {
          description += "swap token0 for exactly " + formatTokenAmount(testCase.amount1) + " token1" + priceClause;
        } else {
          description += "swap token1 for exactly " + formatTokenAmount(testCase.amount0) + " token0" + priceClause;
        }
      } else {
        if (testCase.zeroForOne) {
          description += "swap exactly " + formatTokenAmount(testCase.amount0) + " token0 for token1" + priceClause;
        } else {
          description += "swap exactly " + formatTokenAmount(testCase.amount1) + " token1 for token0" + priceClause;
        }
      }
    } else {
      if (testCase.zeroForOne) {
        description += "swap token0 for token1" + priceClause;
      } else {
        description += "swap token1 for token0" + priceClause;
      }
    }

    return description;
  }

  private String formatTokenAmount(BigInteger num) {
    return new BigDecimal(num).divide(BigDecimal.valueOf(10).pow(18), MathContext.DECIMAL128).setScale(4, CEILING).toPlainString();
  }

  private String formatPrice(BigInteger price) {
    return new BigDecimal(price).divide(BigDecimal.valueOf(2).pow(96), MathContext.DECIMAL128).pow(2).setScale(4, CEILING).toPlainString();
  }

  private void executeSwap(SwapTestCase testCase) {
    if (testCase.exactOut != null) {
      if (testCase.exactOut) {
        if (testCase.zeroForOne) {
          swap0ForExact1(testCase.amount1, alice, testCase.sqrtPriceLimit);
        } else {
          swap1ForExact0(testCase.amount0, alice, testCase.sqrtPriceLimit);
        }
      } else {
        if (testCase.zeroForOne) {
          swapExact0For1(testCase.amount0, alice, testCase.sqrtPriceLimit);
        } else {
          swapExact1For0(testCase.amount1, alice, testCase.sqrtPriceLimit);
        }
      }
    } else {
      if (testCase.zeroForOne) {
        swapToLowerPrice(alice, testCase.sqrtPriceLimit, IntUtils.TWO_POW_160.toString());
      } else {
        swapToHigherPrice(alice, testCase.sqrtPriceLimit, IntUtils.TWO_POW_160.toString());
      }
    }
  }
}