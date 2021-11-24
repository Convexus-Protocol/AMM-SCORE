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

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

public class PoolData {
  // The first token of the given pool
  public Address tokenA;
  // The second token of the given pool
  public Address tokenB;
  // The fee level of the pool
  public int fee;

  public PoolData(Address tokenA, Address tokenB, int fee) {
    this.tokenA = tokenA;
    this.tokenB = tokenB;
    this.fee = fee;
  }

  public static PoolData readObject (ObjectReader reader) {
    Address tokenA = reader.readAddress();
    int fee = reader.readInt();
    Address tokenB = reader.readAddress();
    return new PoolData(tokenA, tokenB, fee);
  }

  public static void writeObject(ObjectWriter w, PoolData obj) {
    w.write(obj.tokenA);
    w.write(obj.fee);
    w.write(obj.tokenB);
}
}