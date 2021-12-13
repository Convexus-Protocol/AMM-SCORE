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

package exchange.convexus.factory;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

public class ConvexusPoolDeployerParameters {
    public Address factory;
    public Address token0;
    public Address token1;
    public int fee;
    public int tickSpacing;
    
    public static void writeObject(ObjectWriter w, ConvexusPoolDeployerParameters obj) {
        w.write(obj.factory);
        w.write(obj.token0);
        w.write(obj.token1);
        w.write(obj.fee);
        w.write(obj.tickSpacing);
    }

    public static ConvexusPoolDeployerParameters readObject(ObjectReader r) {
        return new ConvexusPoolDeployerParameters(
            r.readAddress(), // factory, 
            r.readAddress(), // token0, 
            r.readAddress(), // token1, 
            r.readInt(), // fee, 
            r.readInt() // tickSpacing
        );
    }

    public ConvexusPoolDeployerParameters (
        Address factory,
        Address token0,
        Address token1,
        int fee,
        int tickSpacing
    ) {
        this.factory = factory;
        this.token0 = token0;
        this.token1 = token1;
        this.fee = fee;
        this.tickSpacing = tickSpacing;
    }
}
