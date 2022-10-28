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
import exchange.convexus.core.interfaces.callback.IConvexusMintCallback;
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
  implements IConvexusLiquidityManagement,
             IConvexusMintCallback
 {
  // ================================================
  // Consts
  // ================================================
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
        "createAndInitializePoolIfNecessary: token0 < token1");

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
    BigInteger amount0Desired,
    BigInteger amount1Desired,
    Address recipient,
    BigInteger deadline
  ) {
    Address pool = createAndInitializePoolIfNecessary(token0, token1, fee, sqrtPriceX96);

    MintParams params = new MintParams(
      token0, 
      token1, 
      fee, 
      tickLower, 
      tickUpper,
      amount0Desired,
      amount1Desired,
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
   * @notice Called to `Context.getCaller()` after minting liquidity to a position from ConvexusPool#mint.
   * @dev In the implementation you must pay the pool tokens owed for the minted liquidity.
   * The caller of this method must be checked to be a ConvexusPool deployed by the canonical ConvexusFactory.
   * @param amount0Owed The amount of token0 due to the pool for the minted liquidity
   * @param amount1Owed The amount of token1 due to the pool for the minted liquidity
   * @param data Any data passed through by the caller via the mint call
   */
  @External
  public void convexusMintCallback (
    BigInteger amount0Owed,
    BigInteger amount1Owed,
    byte[] data
  ) {
    // Context.println("[Callback] paying " + amount0Owed + " / " + amount1Owed + " to " + Context.call(Context.getCaller(), "name"));
    this.liquidityMgr.convexusMintCallback(amount0Owed, amount1Owed, data);
  }

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
