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

package exchange.convexus.initializer;

import java.math.BigInteger;

import exchange.convexus.utils.AddressUtils;
import exchange.convexus.pool.Slot0;
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
        Address _factory
    ) {
        // final Address caller = Context.getCaller();
        this.name = "Convexus Pool Initializer";
        this.factory = _factory;
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
        
        Address pool = (Address) Context.call(factory, "getPool", token0, token1, fee);

        if (pool == null) {
            pool = (Address) Context.call(factory, "createPool", token0, token1, fee);
            Context.call(pool, "initialize", sqrtPriceX96);
        } else {
            var slot0 = (Slot0) Context.call(pool, "slot0");
            BigInteger sqrtPriceX96Existing = slot0.sqrtPriceX96;
            if (sqrtPriceX96Existing.equals(BigInteger.ZERO)) {
                Context.call(pool, "initialize", sqrtPriceX96);
            }
        }

        return pool;
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
