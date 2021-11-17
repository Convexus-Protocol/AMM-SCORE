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

package exchange.convexus.positionmgr;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import exchange.convexus.factory.ConvexusFactory;
import exchange.convexus.initializer.ConvexusPoolInitializer;
import exchange.convexus.positiondescriptor.NonfungibleTokenPositionDescriptor;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;
import exchange.convexus.utils.ConvexusTest;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.ScoreSpy;
import score.Context;

public class NonFungiblePositionManagerTest extends ConvexusTest {

  ScoreSpy<NonfungibleTokenPositionDescriptor> positiondescriptor;
  ScoreSpy<NonFungiblePositionManager> positionmgr;
  ScoreSpy<ConvexusFactory> factory;
  ScoreSpy<ConvexusPoolInitializer> initializer;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;

  void setup_tokens () throws Exception {
    sicx = deploy_sicx();
    usdc = deploy_usdc();
    
    // Transfer some funds to Alice
    sicx.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", alice.getAddress(), IntUtils.MAX_UINT256);
    // Transfer some funds to Bob
    sicx.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
    usdc.invoke(owner, "mintTo", bob.getAddress(), IntUtils.MAX_UINT256);
  }

  void setup_initializer () throws Exception {
    initializer = deploy_initializer(factory.getAddress());
  }

  void setup_positionmgr () throws Exception {
    File directory = new File("./");
    Context.println(directory.getAbsolutePath());
    
    factory = deploy_factory();
    // factory.invoke(owner, "setPoolContract", Files.readAllBytes(Paths.get("../Convexus-Pool/build/libs/Convexus-Pool-0.9.1-optimized.jar")));
    positiondescriptor = deploy_positiondescriptor();
    positionmgr = deploy_positionmgr(factory.getAddress(), positiondescriptor.getAddress());
  }
}
