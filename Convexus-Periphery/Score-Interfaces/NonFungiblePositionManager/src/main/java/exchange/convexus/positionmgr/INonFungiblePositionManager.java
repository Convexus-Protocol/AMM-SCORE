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

package exchange.convexus.positionmgr;

import java.math.BigInteger;
import score.Address;
import score.Context;

public class INonFungiblePositionManager {

  // Write methods

  // ReadOnly methods
  public static PositionInformation positions (
    Address positionManager, 
    BigInteger tokenId
  ) {
    return PositionInformation.fromMap(
      Context.call(positionManager, "positions", tokenId)
    );
  }

  public static Address factory (Address positionManager) {
    return (Address) Context.call(positionManager, "factory");
  }

  public static MintResult mint (
    Address positionManager, 
    MintParams params
  ) {
    return MintResult.fromMap(
      Context.call(positionManager, "mint", params)
    );
  }
}
