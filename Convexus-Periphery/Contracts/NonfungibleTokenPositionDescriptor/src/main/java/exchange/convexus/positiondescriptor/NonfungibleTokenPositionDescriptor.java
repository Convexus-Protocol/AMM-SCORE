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

package exchange.convexus.positiondescriptor;

import java.math.BigInteger;

import exchange.convexus.pool.Slot0;
import exchange.convexus.positionmgr.PositionInformation;
import score.Address;
import score.Context;
import score.annotation.External;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import exchange.convexus.librairies.PoolAddress;

/// @title Describes NFT token positions
/// @notice Produces a string containing the data URI for a JSON metadata string
public class NonfungibleTokenPositionDescriptor {

  public NonfungibleTokenPositionDescriptor () {
    // Nothing to do
  }

  /**
   * Returns a tokenURI for a given tokenID
   * @param positionManager
   * @param tokenId
   * @return
   */
  @External(readonly = true)
  public String tokenURI (Address positionManager, BigInteger tokenId) {
    var position = PositionInformation.fromMap(Context.call(positionManager, "positions", tokenId));
    var factory = (Address) Context.call(positionManager, "factory");

    PoolAddress.PoolKey poolKey = new PoolAddress.PoolKey(position.token0, position.token1, position.fee);
    Address pool = PoolAddress.getPool(factory, poolKey);

    Boolean _flipRatio = false; // TODO: flipRatio implementation
    Address quoteTokenAddress = !_flipRatio ? position.token1 : position.token0;
    Address baseTokenAddress = !_flipRatio ? position.token0 : position.token1;
    var slot0 = Slot0.fromMap(Context.call(pool, "slot0"));

    JsonObject tokenURI = Json.object()
      .add("tokenId", tokenId.toString(10))
      .add("quoteTokenAddress", quoteTokenAddress.toString())
      .add("baseTokenAddress", baseTokenAddress.toString())
      .add("quoteTokenSymbol", (String) Context.call(quoteTokenAddress, "symbol"))
      .add("baseTokenSymbol", (String) Context.call(baseTokenAddress, "symbol"))
      .add("quoteTokenDecimals", ((BigInteger) Context.call(quoteTokenAddress, "decimals")).toString(10))
      .add("baseTokenDecimals", ((BigInteger) Context.call(baseTokenAddress, "decimals")).toString(10))
      .add("flipRatio", _flipRatio.toString())
      .add("tickLower", Integer.toString(position.tickLower))
      .add("tickUpper", Integer.toString(position.tickUpper))
      .add("tickCurrent", Integer.toString(slot0.tick))
      .add("tickSpacing", ((BigInteger) Context.call(pool, "tickSpacing")).toString(10))
      .add("fee",       Integer.toString(position.fee))
      .add("poolAddress", pool.toString())
    ;

    return tokenURI.toString();
  }
}
