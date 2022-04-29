package exchange.convexus.poolreadonly;

import java.math.BigInteger;
import exchange.convexus.pool.PairAmounts;
import score.Address;
import score.Context;

public class IConvexusPoolReadOnly {

  public static PairAmounts swap (
    Address readonlyPool,
    Address pool,
    Address recipient,
    boolean zeroForOne,
    BigInteger amountSpecified,
    BigInteger sqrtPriceLimitX96,
    byte[] data
  ) {
    return PairAmounts.fromMap(Context.call(readonlyPool, "swap", pool, recipient, zeroForOne, amountSpecified, sqrtPriceLimitX96, data));
  }
}
