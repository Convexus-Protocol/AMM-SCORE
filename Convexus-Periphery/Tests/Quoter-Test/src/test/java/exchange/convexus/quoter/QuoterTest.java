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

package exchange.convexus.quoter;

import exchange.convexus.factory.ConvexusFactoryMock;
import exchange.convexus.utils.ConvexusTest;
import exchange.convexus.utils.ScoreSpy;

public class QuoterTest extends ConvexusTest {

  ScoreSpy<ConvexusFactoryMock> factory;
  ScoreSpy<Quoter> quoter;

  void setup_quoter () throws Exception {
    factory = deploy_factory();
    quoter = deploy_quoter(factory.getAddress());
  }
}
