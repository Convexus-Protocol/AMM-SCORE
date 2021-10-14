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

package exchange.switchy.pool;

import exchange.switchy.utils.SwitchyTest;
import score.Address;
import exchange.switchy.factory.SwitchyFactory;
import exchange.switchy.testtokens.Sicx;
import exchange.switchy.testtokens.Usdc;
import exchange.switchy.utils.ScoreSpy;

public class SwitchyPoolTest extends SwitchyTest {

  ScoreSpy<SwitchyPool> pool;
  ScoreSpy<SwitchyFactory> factory;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;

  void setup_pool (Address factory, int fee, int tickSpacing) throws Exception {
    sicx = deploy_sicx();
    usdc = deploy_usdc();
    pool = deploy_pool(sicx.getAddress(), usdc.getAddress(), factory, fee, tickSpacing);
  }

  void setup_factory () throws Exception {
    factory = deploy_factory();
  }
}
