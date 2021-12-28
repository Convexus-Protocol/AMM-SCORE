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

package exchange.convexus.initializer;

import exchange.convexus.utils.ScoreSpy;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import score.Address;

import exchange.convexus.factory.ConvexusFactoryMock;
import exchange.convexus.factory.ConvexusFactoryUtils;
import exchange.convexus.utils.ConvexusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConvexusPoolInitializerUtils {

  // Mock createAndInitializePoolIfNecessary until SCORE deployers supported by unittest
  public static void createAndInitializePoolIfNecessary (
    Class<?> poolClass, 
    Account from, 
    ScoreSpy<ConvexusFactoryMock> factory,
    Address token0, 
    Address token1, 
    int fee, 
    BigInteger price, 
    int tickSpacing
  ) {
    // initializer.invoke(owner, "createAndInitializePoolIfNecessary", 
    //   sicx.getAddress(),
    //   usdc.getAddress(),
    //   fee,
    //   price
    // );
    try {
      ScoreSpy<?> pool = ConvexusTest.deploy(poolClass, token0, token1, factory.getAddress(), fee, tickSpacing);
      pool.invoke(from, "initialize", price);
      ConvexusFactoryUtils.createPool(factory, from, token0, token1, fee, pool.getAddress());
    } catch (Exception e) {
      assertEquals(e.getMessage(), "");
    }
  }

}
