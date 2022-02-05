package exchange.convexus.interfaces.cxs;

import java.math.BigInteger;

import score.Address;
import score.Context;

public class ICXS {
  public static BigInteger getPriorVotes(Address targetAddress, Address account, long blockNumber) {
    return (BigInteger) Context.call(targetAddress, "getPriorVotes", account, blockNumber);
  }
}
