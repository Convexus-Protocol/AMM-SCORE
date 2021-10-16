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

import exchange.convexus.utils.ConvexusTest;
import score.Address;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import exchange.convexus.callee.ConvexusCallee;
import exchange.convexus.factory.ConvexusFactory;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;
import exchange.convexus.utils.ScoreSpy;

public class ConvexusPoolTest extends ConvexusTest {

  ScoreSpy<ConvexusPool> pool;
  ScoreSpy<ConvexusFactory> factory;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<ConvexusCallee> callee;

  void setup_pool (Address factory, int fee, int tickSpacing) throws Exception {
    sicx = deploy_sicx();
    usdc = deploy_usdc();
    pool = deploy_pool(sicx.getAddress(), usdc.getAddress(), factory, fee, tickSpacing);
    callee = deploy_callee();
  }

  void setup_factory () throws Exception {
    factory = deploy_factory();
  }
  
  protected BigInteger encodePriceSqrt (BigInteger reserve1, BigInteger reserve0) {
    return new BigDecimal(reserve1).divide(new BigDecimal(reserve0), MathContext.DECIMAL128).sqrt(MathContext.DECIMAL128).multiply(BigDecimal.valueOf(2).pow(96)).toBigInteger();
  }

  protected int getMinTick(int tickSpacing) {
    return ((int) Math.ceil(-887272 / tickSpacing)) * tickSpacing;
  }

  protected int getMaxTick(int tickSpacing) {
    return ((int) Math.floor(887272 / tickSpacing)) * tickSpacing;
  }
}
