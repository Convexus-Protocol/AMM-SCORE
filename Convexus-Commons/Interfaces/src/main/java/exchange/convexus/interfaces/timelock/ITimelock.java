package exchange.convexus.interfaces.timelock;

import java.math.BigInteger;

import exchange.convexus.structs.MethodCall;
import score.Address;
import score.Context;

public class ITimelock {
  public static BigInteger delay (Address timelock) {
    return (BigInteger) Context.call(timelock, "delay");
  }

  public static boolean queuedTransactions (Address timelock, byte[] hash) {
    return (boolean) Context.call(timelock, "queuedTransactions", hash);
  }

  public static void queueTransaction (Address timelock, MethodCall call, BigInteger eta) {
    Context.call(timelock, "queueTransaction", call, eta);
  }

  public static Object executeTransaction (Address timelock, MethodCall call, BigInteger eta) {
    return Context.call(timelock, "executeTransaction", call, eta);
  }

  public static void cancelTransaction(Address timelock, MethodCall call, BigInteger eta) {
    Context.call(timelock, "cancelTransaction", call, eta);
  }

  public static BigInteger GRACE_PERIOD(Address timelock) {
    return (BigInteger) Context.call(timelock, "GRACE_PERIOD");
  }
}
