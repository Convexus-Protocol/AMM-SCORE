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

package exchange.convexus.initializer;

import exchange.convexus.mocks.factory.ConvexusFactoryMock;
import exchange.convexus.periphery.initializer.ConvexusPoolInitializer;
import exchange.convexus.test.ConvexusTest;
import exchange.convexus.utils.ScoreSpy;

public class ConvexusPoolInitializerTest extends ConvexusTest {

  ScoreSpy<ConvexusPoolInitializer> initializer;
  ScoreSpy<ConvexusFactoryMock> factory;

  void setup_initializer () throws Exception {
    factory = deploy_factory();
    initializer = deploy_initializer(factory.getAddress());
  }
}
