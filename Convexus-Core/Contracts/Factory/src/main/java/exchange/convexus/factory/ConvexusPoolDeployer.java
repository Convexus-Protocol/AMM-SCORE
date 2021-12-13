/*
 * Copyright 2021 ICONation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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