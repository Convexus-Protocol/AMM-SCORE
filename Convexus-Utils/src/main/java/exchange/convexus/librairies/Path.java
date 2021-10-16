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

import java.util.Arrays;

import exchange.convexus.utils.BytesUtils;
import score.Address;
import score.ObjectReader;

public class Path {
  
  /// @dev The length of the bytes encoded address
  final private static int ADDR_SIZE = Address.LENGTH;
  /// @dev The length of the bytes encoded fee
  final private static int FEE_SIZE = BytesUtils.INT_SIZE;
  /// @dev The offset of a single token address and pool fee
  final private static int NEXT_OFFSET = ADDR_SIZE + FEE_SIZE;
  /// @dev The offset of an encoded pool key
  final private static int POP_OFFSET = NEXT_OFFSET + ADDR_SIZE;
  /// @dev The minimum length of an encoding that contains 2 or more pools
  final private static int MULTIPLE_POOLS_MIN_LENGTH = POP_OFFSET + NEXT_OFFSET;

  /**
   * @notice Returns true iff the path contains two or more pools
   * @param path The encoded swap path
   * @return True if path contains two or more pools, otherwise false
   */
  public static boolean hasMultiplePools(byte[] path) {
    return path.length >= MULTIPLE_POOLS_MIN_LENGTH;
  }
  
  /**
   * @notice Returns the number of pools in the path
   * @param path The encoded swap path
   * @return The number of pools in the path
   */
  public static int numPools(byte[] path) {
    // Ignore the first token address. From then on every fee and token offset indicates a pool.
    return ((path.length - ADDR_SIZE) / NEXT_OFFSET);
  }

  /**
   * @notice Decodes the first pool in path
   * @param path The bytes encoded swap path
   * @return PoolData: 
   *  tokenA The first token of the given pool
   *  tokenB The second token of the given pool
   *  fee The fee level of the pool
   */
  public static PoolData decodeFirstPool (ObjectReader reader) {
    Address tokenA = reader.readAddress();
    int fee = reader.readInt();
    Address tokenB = reader.readAddress();
    return new PoolData(tokenA, tokenB, fee);
  }

  /**
   * @notice Gets the segment corresponding to the first pool in the path
   * @param path The bytes encoded swap path
   * @return The segment containing all data necessary to target the first pool in the path
   */
  public static byte[] getFirstPool(byte[] path) {
    return Arrays.copyOfRange(path, 0, POP_OFFSET);
  }

  /**
   * @notice Skips a token + fee element from the buffer and returns the remainder
   * @param path The swap path
   * @return The remaining token + fee elements in the path
   */
  public static byte[] skipToken(byte[] path) {
    return Arrays.copyOfRange(path, NEXT_OFFSET, path.length - NEXT_OFFSET);
  }

}
