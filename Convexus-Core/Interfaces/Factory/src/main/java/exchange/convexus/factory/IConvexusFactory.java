package exchange.convexus.factory;

import score.Address;
import score.Context;

public class IConvexusFactory {

  // ReadOnly methods
  public static Address owner(Address factory) {
    return (Address) Context.call(factory, "owner");
  }

  public static Address getPool (
    Address factory,
    Address token0, 
    Address token1, 
    int fee
  ) {
    return (Address) Context.call(factory, "getPool", token0, token1, fee);
  }

  public static Address createPool (
    Address factory,
    Address token0,
    Address token1,
    int fee
  ) {
    return (Address) Context.call(factory, "createPool", token0, token1, fee);
  }
}
