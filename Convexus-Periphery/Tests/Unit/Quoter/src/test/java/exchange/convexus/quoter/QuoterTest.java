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

package exchange.convexus.quoter;

import exchange.convexus.factory.ConvexusFactoryMock;
import exchange.convexus.periphery.positiondescriptor.NonfungibleTokenPositionDescriptor;
import exchange.convexus.periphery.positionmgr.NonFungiblePositionManager;
import exchange.convexus.periphery.quoter.Quoter;
import exchange.convexus.periphery.router.SwapRouter;
import exchange.convexus.utils.ConvexusTest;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.ScoreSpy;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;

public class QuoterTest extends ConvexusTest {

  ScoreSpy<ConvexusFactoryMock> factory;
  ScoreSpy<Quoter> quoter;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<NonFungiblePositionManager> nft;
  ScoreSpy<SwapRouter> router;

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];

  void setup_router () throws Exception {
    router = deploy_router(factory.getAddress());
  }

  void setup_nft () throws Exception {
    ScoreSpy<NonfungibleTokenPositionDescriptor> positiondescriptor = deploy_positiondescriptor();
    nft = deploy_nft(factory.getAddress(), positiondescriptor.getAddress());
  }

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

  void setup_quoter () throws Exception {
    factory = deploy_factory();
    quoter = deploy_quoter(factory.getAddress());
  }
}
