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

package exchange.convexus.staker;

import java.math.BigInteger;

import exchange.convexus.librairies.PoolAddress;
import exchange.convexus.positionmgr.PositionInformation;
import score.Address;
import score.Context;

public class NFTPositionInfo {

    /// @param factory The address of the Uniswap V3 Factory used in computing the pool address
    /// @param nonfungiblePositionManager The address of the nonfungible position manager to query
    /// @param tokenId The unique identifier of an Uniswap V3 LP token
    /// @return pool The address of the Uniswap V3 pool
    /// @return tickLower The lower tick of the Uniswap V3 position
    /// @return tickUpper The upper tick of the Uniswap V3 position
    /// @return liquidity The amount of liquidity staked
  public static NFTPosition getPositionInfo(
    Address factory, 
    Address nonfungiblePositionManager, 
    BigInteger tokenId
  ) {
    var position = PositionInformation.fromCall(Context.call(nonfungiblePositionManager, "positions", tokenId));
    PoolAddress.PoolKey poolKey = new PoolAddress.PoolKey(position.token0, position.token1, position.fee);
    return new NFTPosition(PoolAddress.getPool(factory, poolKey), position.tickLower, position.tickUpper, position.liquidity);
  }
}
