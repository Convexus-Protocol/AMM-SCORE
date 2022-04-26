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

package exchange.convexus.pool;

import java.math.BigInteger;
import score.Address;
import score.Context;

public class IConvexusPoolCallee {

  // Write methods
  public static void convexusMintCallback (
    Address callee,
    BigInteger amount0Owed,
    BigInteger amount1Owed,
    byte[] data
  ) {
    Context.call(callee, "convexusMintCallback", amount0Owed, amount1Owed, data);
  }

  public static void convexusSwapCallback (
    Address callee,
    BigInteger amount0Delta,
    BigInteger amount1Delta,
    byte[] data
  ) {
    Context.call(callee, "convexusSwapCallback", amount0Delta, amount1Delta, data);
  }

  public static void convexusFlashCallback (
    Address callee,
    BigInteger fee0,
    BigInteger fee1,
    byte[] data
  ) {
    Context.call(callee, "convexusFlashCallback", fee0, fee1, data);
  }
}
