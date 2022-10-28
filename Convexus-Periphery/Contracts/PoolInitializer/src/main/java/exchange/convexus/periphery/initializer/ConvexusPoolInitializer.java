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
import exchange.convexus.core.factory.IConvexusFactory;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.Slot0;
import exchange.convexus.positionmgr.INonFungiblePositionManager;
import exchange.convexus.positionmgr.MintParams;
import exchange.convexus.positionmgr.MintResult;
import score.Address;
import score.Context;
import score.annotation.External;

/**
 * @title Creates and initializes Convexus Pools
 */
public class ConvexusPoolInitializer {

    // ================================================
    // Consts
    // ================================================
    // Contract name
    private final String name;
    private final Address factory;

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     *  
     */
    public ConvexusPoolInitializer(
        Address factory
    ) {
        // final Address caller = Context.getCaller();
        this.name = "Convexus Pool Initializer";
        this.factory = factory;
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
        Address positionManager,
        int tickLower,
        int tickUpper,
        BigInteger amount0Desired,
        BigInteger amount1Desired,
        BigInteger amount0Min,
        BigInteger amount1Min,
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
            amount0Min,
            amount1Min,
            recipient,
            deadline
        );
        MintResult mintResult = INonFungiblePositionManager.mint(positionManager, params);

        return new CreatePoolAndMintResult(pool, mintResult);
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
