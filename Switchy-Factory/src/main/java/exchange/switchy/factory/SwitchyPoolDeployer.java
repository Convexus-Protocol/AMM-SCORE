package exchange.switchy.factory;

import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

public class SwitchyPoolDeployer {

  // ================================================
  // Consts
  // ================================================
  // Contract class name
  private static final String NAME = "SwitchyPoolDeployer";

  // ================================================
  // DB Variables
  // ================================================
  public static final VarDB<SwitchyPoolDeployerParameters> parameters = Context.newVarDB(NAME + "_parameters", SwitchyPoolDeployerParameters.class);

  // ================================================
  // Methods
  // ================================================
  public static Address deploy(byte[] contractBytes, Address factory, Address token0, Address token1, int fee, int tickSpacing) {
    SwitchyPoolDeployer.parameters.set(new SwitchyPoolDeployerParameters(factory, token0, token1, fee, tickSpacing));
    Address pool = Context.deploy(contractBytes);
    SwitchyPoolDeployer.parameters.set(null);
    return pool;
  }

  @External(readonly = true)
  public SwitchyPoolDeployerParameters parameters () {
    return SwitchyPoolDeployer.parameters.get();
  }
}
