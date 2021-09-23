package exchange.switchy.factory;

import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

class Parameters {
  public Address factory;
  public Address getfactory () { return this.factory; }
  public void setfactory (Address v) { this.factory = v; }

  public Address token0;
  public Address gettoken0 () { return this.token0; }
  public void settoken0 (Address v) { this.token0 = v; }

  public Address token1;
  public Address gettoken1 () { return this.token1; }
  public void settoken1 (Address v) { this.token1 = v; }

  public int fee;
  public int getfee () { return this.fee; }
  public void setfee (int v) { this.fee = v; }

  public int tickSpacing;
  public int gettickSpacing () { return this.tickSpacing; }
  public void settickSpacing (int v) { this.tickSpacing = v; }

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

  @External
  public Parameters parameters () {
    return SwitchyPoolDeployer.parameters.get();
  }
}
