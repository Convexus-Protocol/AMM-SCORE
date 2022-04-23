package exchange.convexus.factory;

import score.Address;
import score.Context;

public class IFactory {

  // ReadOnly methods
  public static Address owner(Address factory) {
    return (Address) Context.call(factory, "owner");
  }
}
