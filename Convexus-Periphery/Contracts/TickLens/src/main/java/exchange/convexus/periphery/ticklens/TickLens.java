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

package exchange.convexus.periphery.ticklens;

import java.math.BigInteger;

import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.pool.Tick;
import score.Address;
import score.annotation.External;

/**
 * @title Creates and initializes Convexus Pools
 */
public class TickLens {
    // ================================================
    // Consts
    // ================================================
    // Contract name
    private final String name;

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     */
    public TickLens() {
        this.name = "Convexus Tick Lens";
    }

    private boolean hit (BigInteger bitmap, int index) {
        return bitmap.and(BigInteger.ONE.shiftLeft(index)).compareTo(BigInteger.ZERO) > 0;
    }

    @External(readonly = true)
    public PopulatedTick[] getPopulatedTicksInWord (Address pool, int tickBitmapIndex) {

        // fetch bitmap
        BigInteger bitmap = IConvexusPool.tickBitmap(pool, tickBitmapIndex);

        // calculate the number of populated ticks
        int numberOfPopulatedTicks = 0;
        for (int i = 0; i < 256; i++) {
            if (hit(bitmap, i)) {
                numberOfPopulatedTicks++;
            }
        }

        // fetch populated tick data
        int tickSpacing = IConvexusPool.tickSpacing(pool);
        PopulatedTick[] populatedTicks = new PopulatedTick[numberOfPopulatedTicks];

        for (int i = 0; i < 256; i++) {
            if (hit(bitmap, i)) {
                int populatedTick = ((tickBitmapIndex << 8) + i) * tickSpacing;
                Tick.Info result = IConvexusPool.ticks(pool, populatedTick);
                populatedTicks[--numberOfPopulatedTicks] = new PopulatedTick(
                    populatedTick, 
                    result.liquidityNet, 
                    result.liquidityGross
                );
            }
        }
        
        return populatedTicks;
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }
}
