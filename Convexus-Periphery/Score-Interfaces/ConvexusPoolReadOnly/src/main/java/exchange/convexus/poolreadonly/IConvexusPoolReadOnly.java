package exchange.convexus.poolreadonly;

import java.math.BigInteger;
import exchange.convexus.periphery.poolreadonly.SwapResult;
import score.Address;
import score.Context;

public class IConvexusPoolReadOnly {

  public static SwapResult swap (
    Address readOnlyPool,
    Address pool,
    Address recipient,
    boolean zeroForOne,
    BigInteger amountSpecified,
    BigInteger sqrtPriceLimitX96,
    byte[] data
  ) {
    return SwapResult.fromMap(Context.call(readOnlyPool, "swap", pool, recipient, zeroForOne, amountSpecified, sqrtPriceLimitX96, data));
  }
}
