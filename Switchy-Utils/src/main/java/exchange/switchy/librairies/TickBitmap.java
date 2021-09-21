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

import java.math.BigInteger;

import score.Context;
import score.DictDB;

public class TickBitmap {
  
  // ================================================
  // Consts
  // ================================================
  // Class name
  private static final String NAME = "Ticks";

  // ================================================
  // DB Variables
  // ================================================
  // Returns 256 packed tick initialized boolean values. See TickBitmap for more information
  private final DictDB<Integer, BigInteger> tickBitmap = Context.newDictDB(NAME + "_tickBitmap", BigInteger.class);
  
  // ================================================
  // Methods 
  // ================================================
  
  public class PositionResult {
    public int wordPos;
    public int bitPos;
    public PositionResult (int wordPos, int bitPos) {
      this.wordPos = wordPos;
      this.bitPos = bitPos;
    }
  }

  /**
   * @notice Computes the position in the mapping where the initialized bit for a tick lives
   * @param tick The tick for which to compute the position
   * @return wordPos The key in the mapping containing the word in which the bit is stored
   * @return bitPos The bit position in the word where the flag is stored
   */
  private PositionResult position (int tick) {
    int wordPos = tick >> 8;
    int bitPos = tick % 256;
    return new PositionResult(wordPos, bitPos);
  }

  /**
   * @notice Flips the initialized state for a given tick from false to true, or vice versa
   * @param self The mapping in which to flip the tick
   * @param tick The tick to flip
   * @param tickSpacing The spacing between usable ticks
   */
  public void flipTick (
    int tick,
    int tickSpacing
  ) {
      Context.require(tick % tickSpacing == 0); // ensure that the tick is spaced
      var result = position(tick / tickSpacing);
      int wordPos = result.wordPos;
      int bitPos = result.bitPos;

      BigInteger mask = BigInteger.ONE.shiftLeft(bitPos);
      BigInteger packedTick = this.tickBitmap.get(wordPos);

      this.tickBitmap.set(wordPos, packedTick.xor(mask));
  }
}
