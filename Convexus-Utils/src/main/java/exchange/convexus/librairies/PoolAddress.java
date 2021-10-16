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

package exchange.convexus.librairies;

import exchange.convexus.utils.AddressUtils;
import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class PoolAddress {
  
  public static class PoolKey {
    public Address token0;
    public Address token1;
    public int fee;

    public PoolKey(Address token0, Address token1, int fee) {
      this.token0 = token0;
      this.token1 = token1;
      this.fee = fee;
    }

    public static PoolKey readObject(ObjectReader reader) {
      Address token0 = reader.readAddress();
      Address token1 = reader.readAddress();
      int fee = reader.readInt();
      return new PoolKey(token0, token1, fee);
    }

    public static void writeObject(ObjectWriter w, PoolKey obj) {
      w.write(obj.token0);
      w.write(obj.token1);
      w.write(obj.fee);
    }
  }

  /**
   * @notice Returns PoolKey: the ordered tokens with the matched fee levels
   * @param tokenA The first token of a pool, unsorted
   * @param tokenB The second token of a pool, unsorted
   * @param fee The fee level of the pool
   * @return Poolkey The pool details with ordered token0 and token1 assignments
   */
  public static PoolKey getPoolKey (Address tokenA, Address tokenB, int fee) {
    Address token0 = tokenA;
    Address token1 = tokenB;

    if (AddressUtils.compareTo(tokenA, tokenB) > 0) {
        token0 = tokenB;
        token1 = tokenA;
    }

    return new PoolAddress.PoolKey(token0, token1, fee);
  }

  public static Address getPool (Address factory, PoolKey key) {
    return (Address) Context.call(factory, "getPool", key.token0, key.token1, key.fee);
  }
}
