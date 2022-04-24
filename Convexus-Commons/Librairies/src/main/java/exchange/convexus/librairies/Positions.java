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

import static java.math.BigInteger.ZERO;

import exchange.convexus.utils.BytesUtils;
import score.Address;
import score.Context;
import score.DictDB;

public class Positions {
  // ================================================
  // Consts
  // ================================================
  // Contract class name
  private static final String NAME = "Positions";
  
  // Returns the information about a position by the position's key
  private final DictDB<byte[], Position.Info> positions = Context.newDictDB(NAME + "_positions", Position.Info.class);
  private Position.Info emptyPosition () {
    return new Position.Info(ZERO, ZERO, ZERO, ZERO, ZERO);
  }

  public Position.Info get (byte[] key) {
    var position = this.positions.get(key);
    return position == null ? emptyPosition() : position;
  }

  public void set (byte[] key, Position.Info value) {
    this.positions.set(key, value);
  }

  public static byte[] getKey (
      Address owner,
      int tickLower,
      int tickUpper
  ) {
      return Context.hash("sha3-256", 
          BytesUtils.concat(
              owner.toByteArray(), 
              BytesUtils.intToBytes(tickLower), 
              BytesUtils.intToBytes(tickUpper)
          )
      );
  }
}
