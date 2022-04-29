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

package exchange.convexus.liquidity;

import exchange.convexus.periphery.liquidity.ConvexusLiquidityManagement;
import exchange.convexus.test.ConvexusTest;
import exchange.convexus.utils.ScoreSpy;

public class ConvexusLiquidityManagementTest extends ConvexusTest {

  ScoreSpy<ConvexusLiquidityManagement> pool;

  void setup_liquidity_management () throws Exception {
    pool = deploy_liquidity_management();
  }
}
