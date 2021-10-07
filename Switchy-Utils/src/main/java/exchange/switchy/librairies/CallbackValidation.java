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

package exchange.switchy.librairies;

import score.Address;
import score.Context;

public class CallbackValidation {

  /**
   * @notice Returns the address of a valid Switchy Pool
   * @param factory The contract address of the Switchy factory
   * @param tokenA The contract address of either token0 or token1
   * @param tokenB The contract address of the other token
   * @param fee The fee collected upon every swap in the pool, denominated in hundredths of a bip
   * @return pool The pool contract address
   */
  public static Address verifyCallback (Address factory, Address tokenA, Address tokenB, int fee) {
    return verifyCallback (factory, PoolAddress.getPoolKey(tokenA, tokenB, fee));
  }

  /**
   * @notice Returns the address of a valid Switchy Pool
   * @param factory The contract address of the Switchy factory
   * @param poolKey The identifying key of the V3 pool
   * @return pool The pool contract address
   */
  public static Address verifyCallback (Address factory, PoolAddress.PoolKey poolKey) {
    Address pool = PoolAddress.getPool(factory, poolKey);
    Context.require(Context.getCaller().equals(pool));
    return pool;
  }
}
