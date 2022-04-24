/*
 * Copyright 2022 ICONation
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

package exchange.convexus.librairies;

import score.Address;
import score.Context;

public class CallbackValidation {

  /**
   * @notice Returns the address of a valid Convexus Pool
   * @param factory The contract address of the Convexus factory
   * @param tokenA The contract address of either token0 or token1
   * @param tokenB The contract address of the other token
   * @param fee The fee collected upon every swap in the pool, denominated in hundredths of a bip
   * @return pool The pool contract address
   */
  public static Address verifyCallback (Address factory, Address tokenA, Address tokenB, int fee) {
    return verifyCallback (factory, PoolAddress.getPoolKey(tokenA, tokenB, fee));
  }

  private static String getName (Address address) {
    return (String) Context.call(address, "name");
  }

  /**
   * @notice Returns the address of a valid Convexus Pool
   * @param factory The contract address of the Convexus factory
   * @param poolKey The identifying key of the pool
   * @return pool The pool contract address
   */
  public static Address verifyCallback (Address factory, PoolAddress.PoolKey poolKey) {
    Address pool = PoolAddress.getPool(factory, poolKey);
    Context.require(Context.getCaller().equals(pool), "verifyCallback: failed (" + getName(Context.getCaller()) + " / " + getName(pool) + ")");
    return pool;
  }
}
