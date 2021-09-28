package exchange.switchy.librairies;

import java.math.BigInteger;

import exchange.switchy.utils.TimeUtils;

public class BlockTimestamp {

    /**
     * @notice Returns the block timestamp truncated to seconds.
     */
    public static BigInteger _blockTimestamp () {
      return TimeUtils.nowSeconds();
  }
}
