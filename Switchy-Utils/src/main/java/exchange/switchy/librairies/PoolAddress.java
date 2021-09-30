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

import exchange.switchy.utils.AddressUtils;
import exchange.switchy.utils.ByteReader;
import exchange.switchy.utils.BytesUtils;
import score.Address;
import score.Context;

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

    public static PoolKey fromBytes(ByteReader reader) {
      Address token0 = reader.readAddress();
      Address token1 = reader.readAddress();
      int fee = reader.readInt();
      return new PoolKey(token0, token1, fee);
    }

    public byte[] toBytes() {
      return BytesUtils.concat(
        this.token0.toByteArray(),
        this.token1.toByteArray(),
        BytesUtils.intToBytes(this.fee)
      );
    }
  }

  // TODO: changeme
  private final static byte[] POOL_INIT_CODE_HASH = {(byte) 0xe3, (byte) 0x4f, (byte) 0x19, (byte) 0x9b, (byte) 0x19, (byte) 0xb2, (byte) 0xb4, (byte) 0xf4, (byte) 0x7f, (byte) 0x68, (byte) 0x44, (byte) 0x26, (byte) 0x19, (byte) 0xd5, (byte) 0x55, (byte) 0x52, (byte) 0x7d, (byte) 0x24, (byte) 0x4f, (byte) 0x78, (byte) 0xa3, (byte) 0x29, (byte) 0x7e, (byte) 0xa8, (byte) 0x93, (byte) 0x25, (byte) 0xf8, (byte) 0x43, (byte) 0xf8, (byte) 0x7b, (byte) 0x8b, (byte) 0x54};

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

  /**
   * @notice Deterministically computes the pool address given the factory and PoolKey
   * @param factory The Switchy factory contract address
   * @param key The PoolKey
   * @return pool The contract address of the pool
   */
  public static Address computeAddress (Address factory, PoolKey key) {
    Context.require(AddressUtils.compareTo(key.token0, key.token1) < 0,
      "computeAddress: key.token0 < key.token1");

    byte[] prefix = {(byte) 0xff};
    
    return new Address(
      Context.hash("keccak-256", BytesUtils.concat(
        prefix,
        factory.toByteArray(), 
        Context.hash("keccak-256", BytesUtils.concat(
            key.token0.toByteArray(),
            key.token1.toByteArray(),
            BytesUtils.intToBytes(key.fee)
        )),
        POOL_INIT_CODE_HASH
      ))
    );
  }

}
