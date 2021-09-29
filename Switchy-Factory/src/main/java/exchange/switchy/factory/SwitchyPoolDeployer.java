package exchange.switchy.factory;

import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

class Parameters {
  public Address factory;
  public Address token0;
  public Address token1;
  public int fee;
  public int tickSpacing;

  public Parameters (
    Address factory,
    Address token0,
    Address token1,
    int fee,
    int tickSpacing
  ) {
    this.factory = factory;
    this.token0 = token0;
    this.token1 = token1;
    this.fee = fee;
    this.tickSpacing = tickSpacing;
  }
}

public class SwitchyPoolDeployer {

  // ================================================
  // Consts
  // ================================================
  // Contract class name
  private static final String NAME = "SwitchyPoolDeployer";

  // TODO: Fill bytes
  private final static byte[] contractBytes = {};

  // ================================================
  // DB Variables
  // ================================================
  private static final VarDB<Parameters> parameters = Context.newVarDB(NAME + "_parameters", Parameters.class);

  // ================================================
  // Methods
  // ================================================
  public static Address deploy(Address factory, Address token0, Address token1, int fee, int tickSpacing) {
    SwitchyPoolDeployer.parameters.set(new Parameters(factory, token0, token1, fee, tickSpacing));
    Address pool = Context.deploy(contractBytes);
    SwitchyPoolDeployer.parameters.set(null);
    return pool;
  }

  @External(readonly = true)
  public Parameters parameters () {
    return SwitchyPoolDeployer.parameters.get();
  }
}
