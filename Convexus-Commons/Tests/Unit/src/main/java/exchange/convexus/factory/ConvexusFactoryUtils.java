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

package exchange.convexus.factory;

import org.mockito.ArgumentCaptor;

import exchange.convexus.utils.ScoreSpy;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.Account;

import score.Address;
import exchange.convexus.core.contracts.factory.ConvexusFactory;
import exchange.convexus.factory.ConvexusFactoryUtils;

public class ConvexusFactoryUtils {

  public static Address createPool (
    ScoreSpy<ConvexusFactoryMock> factory,
    Account from,
    Address tokenA,
    Address tokenB,
    int fee,
    Address pool
  ) {
    reset(factory.spy);
    factory.invoke(from, "createPool", tokenA, tokenB, fee, pool);
    // Get pool address from PoolCreated event
    ArgumentCaptor<Address> _token0 = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Address> _token1 = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _fee = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickSpacing = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _pool = ArgumentCaptor.forClass(Address.class);
    verify(factory.spy).PoolCreated(_token0.capture(), _token1.capture(), _fee.capture(), _tickSpacing.capture(), _pool.capture());
    return _pool.getValue();
  }
  
  public static Address createPool (
    ScoreSpy<ConvexusFactory> factory,
    Account from,
    Address tokenA,
    Address tokenB,
    int fee
  ) {
    reset(factory.spy);
    factory.invoke(from, "createPool", tokenA, tokenB, fee);
    // Get pool address from PoolCreated event
    ArgumentCaptor<Address> _token0 = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Address> _token1 = ArgumentCaptor.forClass(Address.class);
    ArgumentCaptor<Integer> _fee = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> _tickSpacing = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Address> _pool = ArgumentCaptor.forClass(Address.class);
    verify(factory.spy).PoolCreated(_token0.capture(), _token1.capture(), _fee.capture(), _tickSpacing.capture(), _pool.capture());
    return _pool.getValue();
  }
}
