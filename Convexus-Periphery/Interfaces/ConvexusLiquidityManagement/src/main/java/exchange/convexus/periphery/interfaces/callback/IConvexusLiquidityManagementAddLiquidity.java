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

package exchange.convexus.periphery.interfaces.callback;

import java.math.BigInteger;
import exchange.convexus.core.interfaces.callback.IConvexusMintCallback;
import exchange.convexus.periphery.liquidity.AddLiquidityParams;
import exchange.convexus.periphery.liquidity.AddLiquidityResult;

public interface IConvexusLiquidityManagementAddLiquidity 
  extends IConvexusMintCallback
{
  /**
   * @notice Called to `Context.getCaller()` after minting liquidity to a position from ConvexusPool#mint.
   * @dev In the implementation you must pay the pool tokens owed for the minted liquidity.
   * The caller of this method must be checked to be a ConvexusPool deployed by the canonical ConvexusFactory.
   * @param amount0Owed The amount of token0 due to the pool for the minted liquidity
   * @param amount1Owed The amount of token1 due to the pool for the minted liquidity
   * @param data Any data passed through by the caller via the mint call
   */
  // @External
  public void convexusMintCallback (
    BigInteger amount0Owed,
    BigInteger amount1Owed,
    byte[] data
  );

  /**
   * @notice Add liquidity to an initialized pool
   * @dev Liquidity must have been provided beforehand
   */
  // @External
  public AddLiquidityResult addLiquidity (AddLiquidityParams params);
}