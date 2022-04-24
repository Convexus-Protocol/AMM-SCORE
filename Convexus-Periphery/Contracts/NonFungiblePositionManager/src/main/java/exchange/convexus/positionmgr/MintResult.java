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

package exchange.convexus.positionmgr;

import java.math.BigInteger;

public class MintResult {
  // The ID of the token that represents the minted position
  public BigInteger tokenId;
  // The amount of liquidity for this position
  public BigInteger liquidity;
  // The amount of token0
  public BigInteger amount0;
  // The amount of token1
  public BigInteger amount1;
  
  public MintResult(
    BigInteger tokenId, 
    BigInteger liquidity, 
    BigInteger amount0, 
    BigInteger amount1) {
      this.tokenId = tokenId;
      this.liquidity = liquidity;
      this.amount0 = amount0;
      this.amount1 = amount1;
  }
}