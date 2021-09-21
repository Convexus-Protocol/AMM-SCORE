package exchange.switchy.librairies;

import java.math.BigInteger;
import static exchange.switchy.utils.MathUtils.gt;

public class UnsafeMath {
  public static BigInteger divRoundingUp(BigInteger x, BigInteger y) {
    return x.divide(y).add(gt(x.mod(y), BigInteger.ZERO));
  }
}
