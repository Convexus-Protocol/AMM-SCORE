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

package exchange.convexus.factory;

import exchange.convexus.core.contracts.factory.ConvexusFactory;
import exchange.convexus.utils.AddressUtils;
import score.Address;
import score.Context;
import score.annotation.External;

public class ConvexusFactoryMock extends ConvexusFactory {

  public ConvexusFactoryMock() {}
  
  @Override
  @External
  public Address createPool (
      Address tokenA,
      Address tokenB,
      int fee
  ) {
      Context.revert("Unimplemented");
      return null;
  }

  @External
  public Address createPool (
      Address tokenA,
      Address tokenB,
      int fee,
      // We don't really deploy it for unittests, pass it as an argument
      Address pool
  ) {
    Context.require(!tokenA.equals(tokenB),
        "createPool: tokenA must be different from tokenB");

    Address token0 = tokenA;
    Address token1 = tokenB;

    if (AddressUtils.compareTo(tokenA, tokenB) >= 0) {
        token0 = tokenB;
        token1 = tokenA;
    }

    Context.require(!token0.equals(AddressUtils.ZERO_ADDRESS),
        "createPool: token0 cannot be ZERO_ADDRESS");

    int tickSpacing = this.feeAmountTickSpacing.getOrDefault(fee, 0);
    Context.require(tickSpacing != 0, 
        "createPool: tickSpacing cannot be 0");

    Context.require(getPool.at(token0).at(token1).get(fee) == null, 
        "createPool: pool already exists");

    // Address pool = ConvexusPoolDeployer.deploy (
    //     this.poolContract.get(), 
    //     Context.getAddress(), 
    //     token0, token1, fee, tickSpacing
    // );

    this.getPool.at(token0).at(token1).set(fee, pool);
    // populate mapping in the reverse direction, deliberate choice to avoid the cost of comparing addresses
    this.getPool.at(token1).at(token0).set(fee, pool);

    this.PoolCreated(token0, token1, fee, tickSpacing, pool);

    return pool;
  }
}
