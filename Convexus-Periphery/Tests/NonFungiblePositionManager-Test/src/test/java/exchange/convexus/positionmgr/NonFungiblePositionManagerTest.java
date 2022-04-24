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

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.mockito.ArgumentCaptor;

import exchange.convexus.factory.ConvexusFactoryMock;
import exchange.convexus.initializer.ConvexusPoolInitializer;
import exchange.convexus.librairies.PairAmounts;
import exchange.convexus.pool.ConvexusPool;
import exchange.convexus.positiondescriptor.NonfungibleTokenPositionDescriptor;
import exchange.convexus.router.SwapRouter;
import exchange.convexus.testtokens.Sicx;
import exchange.convexus.testtokens.Usdc;
import exchange.convexus.utils.ConvexusTest;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.ScoreSpy;
import score.Address;

public class NonFungiblePositionManagerTest extends ConvexusTest {

  ScoreSpy<NonfungibleTokenPositionDescriptor> positiondescriptor;
  ScoreSpy<ConvexusFactoryMock> factory;
  ScoreSpy<ConvexusPoolInitializer> initializer;
  ScoreSpy<Sicx> sicx;
  ScoreSpy<Usdc> usdc;
  ScoreSpy<NonFungiblePositionManager> nft;
  ScoreSpy<ConvexusPool> pool;
  ScoreSpy<SwapRouter> router;

  final int TICK_SPACINGS[] = {10, 60, 200};
  final int FEE_AMOUNTS[] = {500, 3000, 10000};
  final int LOW = 0;
  final int MEDIUM = 1;
  final int HIGH = 2;
  int FEE = FEE_AMOUNTS[MEDIUM];
  int tickSpacing = TICK_SPACINGS[MEDIUM];

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

  void setup_nft () throws Exception {
    factory = deploy_factory();
    router = deploy_router(factory.getAddress());
    positiondescriptor = deploy_positiondescriptor();
    nft = deploy_nft(factory.getAddress(), positiondescriptor.getAddress());
  }

  protected void increaseLiquidity (
    Account from,
    BigInteger tokenId, 
    BigInteger amount0Desired, 
    BigInteger amount1Desired, 
    BigInteger amount0Min,
    BigInteger amount1Min, 
    BigInteger deadline
  ) {
    nft.invoke(from, "increaseLiquidity", new IncreaseLiquidityParams(
      tokenId,
      amount0Desired,
      amount1Desired,
      amount0Min,
      amount1Min,
      deadline
    ));
  }
  
  protected PairAmounts collect (
    Account from,
    BigInteger tokenId,
    Address recipient,
    BigInteger amount0Max,
    BigInteger amount1Max
  ) {
    reset(nft.spy);
    nft.invoke(from, "collect", new CollectParams(
      tokenId,
      recipient,
      amount0Max,
      amount1Max
    ));
    
    // Get Collect event
    ArgumentCaptor<BigInteger> _tokenId = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<Address> _recipient = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<BigInteger> _amount0Collect = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> _amount1Collect = ArgumentCaptor.forClass(BigInteger.class);
    verify(nft.spy).Collect(_tokenId.capture(), _recipient.capture(), _amount0Collect.capture(), _amount1Collect.capture());

    return new PairAmounts(_amount0Collect.getValue(), _amount1Collect.getValue());
  }
  
  protected void burn (
    Account from,
    BigInteger tokenId
  ) {
    nft.invoke(from, "burn", tokenId);
  }
  
  protected void transferFrom (
    Account caller,
    Address from,
    Address to,
    BigInteger tokenId
  ) {
    nft.invoke(caller, "transferFrom", from, to, tokenId);
  }
  
  protected void approve (Account caller, Address to, BigInteger tokenId) {
    nft.invoke(caller, "approve", to, tokenId);
  }
}
