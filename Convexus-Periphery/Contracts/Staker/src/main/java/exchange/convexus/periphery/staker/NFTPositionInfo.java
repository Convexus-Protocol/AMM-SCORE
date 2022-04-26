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

package exchange.convexus.periphery.staker;

import java.math.BigInteger;
import exchange.convexus.periphery.librairies.PoolAddressLib;
import exchange.convexus.pool.PoolAddress.PoolKey;
import exchange.convexus.positionmgr.INonFungiblePositionManager;
import exchange.convexus.positionmgr.PositionInformation;
import exchange.convexus.staker.NFTPosition;
import score.Address;

public class NFTPositionInfo {
    /// @param factory The address of the Convexus Factory used in computing the pool address
    /// @param nonfungiblePositionManager The address of the nonfungible position manager to query
    /// @param tokenId The unique identifier of an Convexus LP token
    /// @return pool The address of the Convexus pool
    /// @return tickLower The lower tick of the Convexus position
    /// @return tickUpper The upper tick of the Convexus position
    /// @return liquidity The amount of liquidity staked
  public static NFTPosition getPositionInfo (
    Address factory, 
    Address nonfungiblePositionManager, 
    BigInteger tokenId
  ) {
    PositionInformation position = INonFungiblePositionManager.positions(nonfungiblePositionManager, tokenId);
    PoolKey poolKey = new PoolKey(position.token0, position.token1, position.fee);
    return new NFTPosition (PoolAddressLib.getPool(factory, poolKey), position.tickLower, position.tickUpper, position.liquidity);
  }
}
