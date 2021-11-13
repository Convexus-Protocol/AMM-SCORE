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

package exchange.convexus.positiondescriptor;

import java.math.BigInteger;

import exchange.convexus.librairies.PoolAddress;
import exchange.convexus.pool.Slot0;
import exchange.convexus.positionmgr.PositionInformation;
import score.Address;
import score.Context;

/// @title Describes NFT token positions
/// @notice Produces a string containing the data URI for a JSON metadata string
public class NonfungibleTokenPositionDescriptor {

  public String tokenURI (Address positionManager, BigInteger tokenId) {
    var position = PositionInformation.fromCall(Context.call(positionManager, "positions", tokenId));
    var factory = (Address) Context.call(positionManager, "factory");

    PoolAddress.PoolKey poolKey = new PoolAddress.PoolKey(position.token0, position.token1, position.fee);
    Address pool = PoolAddress.getPool(factory, poolKey);

    Boolean _flipRatio = false; // TODO: flipRatio implementation
    Address quoteTokenAddress = !_flipRatio ? position.token1 : position.token0;
    Address baseTokenAddress = !_flipRatio ? position.token0 : position.token1;
    var slot0 = (Slot0) Context.call(pool, "slot0");

    return tokenId.toString(10) + "|" 
         + quoteTokenAddress.toString() + "|" 
         + baseTokenAddress.toString() + "|"
         + _flipRatio.toString() + "|"
         + Integer.toString(position.tickLower) + "|"
         + Integer.toString(position.tickUpper) + "|"
         + Integer.toString(slot0.tick) + "|"
         + Integer.toString(position.fee) + "|"
         + pool.toString() + "|";
  }
}
