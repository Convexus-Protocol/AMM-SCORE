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

package exchange.convexus.periphery.positiondescriptor;

import java.math.BigInteger;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.PoolAddress.PoolKey;
import exchange.convexus.positionmgr.INonFungiblePositionManager;
import exchange.convexus.positionmgr.PositionInformation;
import score.Address;
import score.annotation.External;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import exchange.convexus.interfaces.irc2.IIRC2;
import exchange.convexus.periphery.librairies.PoolAddressLib;

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
    PositionInformation position = INonFungiblePositionManager.positions(positionManager, tokenId);
    Address factory = INonFungiblePositionManager.factory(positionManager);

    PoolKey poolKey = new PoolKey(position.token0, position.token1, position.fee);
    Address pool = PoolAddressLib.getPool(factory, poolKey);

    Boolean _flipRatio = false; // TODO: flipRatio implementation
    Address quoteTokenAddress = !_flipRatio ? position.token1 : position.token0;
    Address baseTokenAddress = !_flipRatio ? position.token0 : position.token1;
    var slot0 = IConvexusPool.slot0(pool);

    JsonObject tokenURI = Json.object()
      .add("tokenId", tokenId.toString(10))
      .add("quoteTokenAddress", quoteTokenAddress.toString())
      .add("baseTokenAddress", baseTokenAddress.toString())
      .add("quoteTokenSymbol", IIRC2.symbol(quoteTokenAddress))
      .add("baseTokenSymbol", IIRC2.symbol(baseTokenAddress))
      .add("quoteTokenDecimals", Integer.toString(IIRC2.decimals(quoteTokenAddress)))
      .add("baseTokenDecimals", Integer.toString(IIRC2.decimals(baseTokenAddress)))
      .add("flipRatio", _flipRatio.toString())
      .add("tickLower", Integer.toString(position.tickLower))
      .add("tickUpper", Integer.toString(position.tickUpper))
      .add("tickCurrent", Integer.toString(slot0.tick))
      .add("tickSpacing", Integer.toString(IConvexusPool.tickSpacing(pool)))
      .add("fee",       Integer.toString(position.fee))
      .add("poolAddress", pool.toString())
    ;

    return tokenURI.toString();
  }
}
