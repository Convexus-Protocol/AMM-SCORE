package exchange.convexus.factory;

import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

public class ConvexusPoolDeployer {

  // ================================================
  // Consts
  // ================================================
  // Contract class name
  private static final String NAME = "ConvexusPoolDeployer";

  // ================================================
  // DB Variables
  // ================================================
  public static final VarDB<ConvexusPoolDeployerParameters> parameters = Context.newVarDB(NAME + "_parameters", ConvexusPoolDeployerParameters.class);

  // ================================================
  // Methods
  // ================================================
  public static Address deploy(byte[] contractBytes, Address factory, Address token0, Address token1, int fee, int tickSpacing) {
    ConvexusPoolDeployer.parameters.set(new ConvexusPoolDeployerParameters(factory, token0, token1, fee, tickSpacing));
    Address pool = Context.deploy(contractBytes);
    ConvexusPoolDeployer.parameters.set(null);
    return pool;
  }

  @External(readonly = true)
  public ConvexusPoolDeployerParameters parameters () {
    return ConvexusPoolDeployer.parameters.get();
  }
}
