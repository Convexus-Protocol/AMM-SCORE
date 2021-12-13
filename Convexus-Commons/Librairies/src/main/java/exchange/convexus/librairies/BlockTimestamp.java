package exchange.convexus.librairies;

import java.math.BigInteger;

import static exchange.convexus.utils.TimeUtils.now;

public class BlockTimestamp {

    /**
     * @notice Returns the block timestamp truncated to seconds.
     */
    public static BigInteger _blockTimestamp () {
      return now();
  }
}
