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

package exchange.convexus.pool;

import java.math.BigInteger;

import exchange.convexus.factory.Parameters;
import score.Address;
import score.annotation.External;

public class ConvexusPoolMock extends ConvexusPool {

  public ConvexusPoolMock(Address token0, Address token1, Address factory, int fee, int tickSpacing) {
    super(new Parameters(factory, token0, token1, fee, tickSpacing));
  }

  @External
  public void setFeeGrowthGlobal0X128 (BigInteger _feeGrowthGlobal0X128) {
    this.feeGrowthGlobal0X128.set(_feeGrowthGlobal0X128);
  }
  
  @External
  public void setFeeGrowthGlobal1X128 (BigInteger _feeGrowthGlobal1X128) {
    this.feeGrowthGlobal1X128.set(_feeGrowthGlobal1X128);
  }
}
