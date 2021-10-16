package exchange.convexus.librairies;

import java.math.BigInteger;

import exchange.convexus.utils.TimeUtils;

public class BlockTimestamp {

    /**
     * @notice Returns the block timestamp truncated to seconds.
     */
    public static BigInteger _blockTimestamp () {
      return TimeUtils.nowSeconds();
  }
}
