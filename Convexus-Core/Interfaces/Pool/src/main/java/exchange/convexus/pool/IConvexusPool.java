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

package exchange.convexus.pool;

import java.math.BigInteger;
import score.Address;
import score.Context;

public class IConvexusPool {

  // Write methods
  public static void mint (
    Address pool, 
    Address recipient, 
    int tickLower, 
    int tickUpper,
    BigInteger amount, 
    byte[] data
  ) {
    Context.call(pool, "mint", recipient, tickLower, tickUpper, amount, data);
  }

  public static void flash (
    Address pool,
    Address recipient,
    BigInteger amount0,
    BigInteger amount1,
    byte[] data
  ) {
    Context.call(pool, "flash", recipient, amount0, amount1, data);
  }

  public static void swap (
    Address pool,
    Address recipient,
    boolean zeroForOne,
    BigInteger amountSpecified,
    BigInteger sqrtPriceLimitX96,
    byte[] data
  ) {
    Context.call(pool, "swap", recipient, zeroForOne, amountSpecified, sqrtPriceLimitX96, data);
  }

  // ReadOnly methods
  public static Address token0 (Address pool) {
    return (Address) Context.call(pool, "token0");
  }

  public static Address token1 (Address pool) {
    return (Address) Context.call(pool, "token1");
  }
}
