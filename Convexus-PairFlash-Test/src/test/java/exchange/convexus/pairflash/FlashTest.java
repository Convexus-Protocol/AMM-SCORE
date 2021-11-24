package exchange.convexus.pairflash;

import java.math.BigInteger;


import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exchange.convexus.liquidity.ConvexusLiquidityUtils;
import score.Context;

public class FlashTest extends PairFlashTest {
  
  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_tokens();
    setup_pairflash();
    setup_positionmgr();
    setup_pool1();
    setup_pool2();
    setup_pool3();
  }

  @Test
  void testCorrectTransferEvents () {
    // choose amountIn to test
    var amount0In = BigInteger.valueOf(1000);
    var amount1In = BigInteger.valueOf(1000);

    // BigInteger fee0 = amount0In.multiply(BigInteger.valueOf(FEE_AMOUNTS[MEDIUM])).divide(BigInteger.valueOf(1000000));
    // BigInteger fee1 = amount1In.multiply(BigInteger.valueOf(FEE_AMOUNTS[MEDIUM])).divide(BigInteger.valueOf(1000000));

    var expectedAmountOut0 = quoteExactInputSingle(alice, usdc.getAddress(), sicx.getAddress(), amount1In, FEE_AMOUNTS[LOW], encodePriceSqrt(20, 10));
    var expectedAmountOut1 = quoteExactInputSingle(alice, sicx.getAddress(), usdc.getAddress(), amount0In, FEE_AMOUNTS[HIGH], encodePriceSqrt(5, 10));

    ConvexusLiquidityUtils.deposit(alice, flash.getAddress(), sicx.score, BigInteger.valueOf(1000));
    ConvexusLiquidityUtils.deposit(alice, flash.getAddress(), usdc.score, BigInteger.valueOf(1000));
    initFlash(alice, sicx.getAddress(), usdc.getAddress(), FEE_AMOUNTS[MEDIUM], amount0In, amount1In, FEE_AMOUNTS[LOW], FEE_AMOUNTS[HIGH]);
  }
}
