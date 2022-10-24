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


package exchange.convexus.periphery.poolreadonly.poolcache;

import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.Position;
import score.Address;
import exchange.convexus.periphery.poolreadonly.cache.DictDBCache;

class PositionsDB extends DictDBCache<byte[], Position.Info> {

  public PositionsDB(Address target) {
    super(target);
  }

  @Override
  public Position.Info getExternal(byte[] positionsKey) {
    return IConvexusPool.positions(this.target, positionsKey);
  }
}

public class PositionsCache {
  // ================================================
  // Consts
  // ================================================
  // Returns the information about a position by the position's key
  private final PositionsDB positions;

  public PositionsCache (Address target) {
    this.positions = new PositionsDB(target);
  }

  public Position.Info get (byte[] key) {
    var position = this.positions.get(key);
    return position == null ? Position.Info.empty() : position;
  }

  public void set (byte[] key, Position.Info value) {
    this.positions.set(key, value);
  }
}
