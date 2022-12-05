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

package exchange.convexus.periphery.initializer;

import exchange.convexus.utils.AddressUtils;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import exchange.convexus.core.factory.IConvexusFactory;
import exchange.convexus.interfaces.irc2.IIRC2ICX;
import exchange.convexus.periphery.interfaces.callback.IConvexusLiquidityManagement;
import exchange.convexus.periphery.liquidity.ConvexusLiquidityManagement;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.Slot0;
import exchange.convexus.positionmgr.INonFungiblePositionManager;
import exchange.convexus.positionmgr.MintParams;
import exchange.convexus.positionmgr.MintResult;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import scorex.io.Reader;
import scorex.io.StringReader;

/**
 * @title Creates and initializes Convexus Pools
 */
public class ConvexusPoolInitializer
  implements IConvexusLiquidityManagement
 {
  // ================================================
  // Consts
  // ================================================
  // Contract class name
  public static final String NAME = "ConvexusPoolInitializer";

  // Contract name
  private final String name;
  private final Address factory;
  private final Address positionManager;

  // Liquidity Manager
  private final ConvexusLiquidityManagement liquidityMgr;

  // ================================================
  // Methods
  // ================================================
  /**
   *  Contract constructor
   */
  public ConvexusPoolInitializer(
    Address factory,
    Address positionManager
  ) {
    this.name = "Convexus Pool Initializer";
    this.factory = factory;
    this.positionManager = positionManager;
    this.liquidityMgr = new ConvexusLiquidityManagement(factory);
  }

  @External
  public Address createAndInitializePoolIfNecessary (
    Address token0,
    Address token1,
    int fee,
    BigInteger sqrtPriceX96
  ) {
    Context.require(AddressUtils.compareTo(token0, token1) < 0, 
        NAME + "::createAndInitializePoolIfNecessary: token0 < token1");

    Address pool = IConvexusFactory.getPool(factory, token0, token1, fee);

    if (pool == null) {
      pool = IConvexusFactory.createPool(factory, token0, token1, fee);
      IConvexusPool.initialize(pool, sqrtPriceX96);
    } else {
      Slot0 slot0 = IConvexusPool.slot0(pool);
      if (slot0.sqrtPriceX96.equals(ZERO)) {
        IConvexusPool.initialize(pool, sqrtPriceX96);
      }
    }

    return pool;
  }

  /**
   * Group PoolInitializer::createAndInitializePoolIfNecessary + NFTManager::mint calls
   */
  @External
  public CreatePoolAndMintResult createAndInitializePoolIfNecessaryAndMintPosition (
    // For the pool creation+initialization
    Address token0,
    Address token1,
    int fee,
    BigInteger sqrtPriceX96,
    // For the position minting
    int tickLower,
    int tickUpper,
    BigInteger amount0,
    BigInteger amount1,
    Address recipient,
    BigInteger deadline
  ) {
    final Address caller = Context.getCaller();

    // Check if user deposited enough funds
    final BigInteger deposited0 = this.deposited(caller, token0);
    final BigInteger deposited1 = this.deposited(caller, token1);
    
    Context.require(deposited0.compareTo(amount0) >= 0,
      NAME + "::createAndInitializePoolIfNecessaryAndMintPosition: not enough deposited 0");
    
    Context.require(deposited1.compareTo(amount1) >= 0,
      NAME + "::createAndInitializePoolIfNecessaryAndMintPosition: not enough deposited 1");
    
    // Create Pool
    Address pool = createAndInitializePoolIfNecessary(token0, token1, fee, sqrtPriceX96);

    // Transfer funds to Position Manager
    IIRC2ICX.transfer(token0, this.positionManager, deposited0, "deposit");
    IIRC2ICX.transfer(token1, this.positionManager, deposited1, "deposit");

    // Mint position
    MintParams params = new MintParams(
      token0, 
      token1, 
      fee, 
      tickLower, 
      tickUpper,
      amount0,
      amount1,
      BigInteger.ZERO,
      BigInteger.ZERO,
      recipient,
      deadline
    );
  
    MintResult mintResult = INonFungiblePositionManager.mint(this.positionManager, params);

    return new CreatePoolAndMintResult(pool, mintResult);
  }

  // ================================================
  // Public variable getters
  // ================================================
  @External(readonly = true)
  public String name() {
    return this.name;
  }

  // ================================================
  // Implements LiquidityManager
  // ================================================
  /**
   * @notice Remove funds from the liquidity manager previously deposited by `Context.getCaller`
   * 
   * @param token The token address to withdraw
   */
  @External
  public void withdraw (Address token) {
    this.liquidityMgr.withdraw(token);
  }

  /**
   * @notice Remove all funds from the liquidity manager previously deposited by `Context.getCaller`
   */
  @External
  public void withdraw_all () {
    this.liquidityMgr.withdraw_all();
  }

  /**
   * @notice Add ICX funds to the liquidity manager
   */
  @External
  @Payable
  public void depositIcx () {
    this.liquidityMgr.depositIcx();
  }
  
  @External
  public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) throws Exception {
    Reader reader = new StringReader(new String(_data));
    JsonValue input = Json.parse(reader);
    JsonObject root = input.asObject();
    String method = root.get("method").asString();
    Address token = Context.getCaller();

    switch (method)
    {
      /**
       * @notice Add IRC2 funds to the liquidity manager
       */
      case "deposit": {
        deposit(_from, token, _value);
        break;
      }

      default:
        Context.revert("tokenFallback: Unimplemented tokenFallback action");
    }
  }

  // @External - this method is external through tokenFallback
  public void deposit(Address caller, Address tokenIn, BigInteger amountIn) {
    this.liquidityMgr.deposit(caller, tokenIn, amountIn);
  }

  // ReadOnly methods
  @External(readonly = true)
  public BigInteger deposited(Address user, Address token) {
    return this.liquidityMgr.deposited(user, token);
  }

  @External(readonly = true)
  public int depositedTokensSize(Address user) {
    return this.liquidityMgr.depositedTokensSize(user);
  }

  @External(readonly = true)
  public Address depositedToken(Address user, int index) {
    return this.liquidityMgr.depositedToken(user, index);
  }

}
