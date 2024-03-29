/*
 * Copyright 2022 Convexus Protocol
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

package exchange.convexus.core.contracts.factory.models;

import exchange.convexus.factory.Parameters;
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
  public final VarDB<Parameters> parameters = Context.newVarDB(NAME + "_parameters", Parameters.class);

  // ================================================
  // Methods
  // ================================================
  public Address deploy(byte[] contractBytes, Address factory, Address token0, Address token1, int fee, int tickSpacing) {
    this.parameters.set(new Parameters(factory, token0, token1, fee, tickSpacing));
    Address pool = Context.deploy(contractBytes);
    this.parameters.set(null);
    return pool;
  }

  public void update (
    Address pool, 
    byte[] contractBytes, 
    Address factory, 
    Address token0, 
    Address token1, 
    int fee, 
    int tickSpacing
  ) {
    this.parameters.set(new Parameters(factory, token0, token1, fee, tickSpacing));
    
    Address result = Context.deploy(pool, contractBytes);
    Context.require(result.equals(pool), 
      NAME + "::update: invalid pool address");

    this.parameters.set(null);
  }

  @External(readonly = true)
  public Parameters parameters () {
    return this.parameters.get();
  }
}
