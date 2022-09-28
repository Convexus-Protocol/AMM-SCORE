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

package exchange.convexus.core.interfaces.ticks;

import java.math.BigInteger;
import exchange.convexus.pool.Tick;
import score.annotation.External;

public interface ITicks {
  // ================================================
  // Methods
  // ================================================
  @External(readonly = true)
  public Tick.Info ticks (int tick);
  
  @External(readonly = true)
  public BigInteger ticksInitializedSize ();

  @External(readonly = true)
  public BigInteger ticksInitialized (int index);
}
