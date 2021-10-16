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

package exchange.convexus.pool;

import java.math.BigInteger;

import score.ObjectReader;
import score.ObjectWriter;

// accumulated protocol fees in token0/token1 units
class ProtocolFees {
    BigInteger token0;
    BigInteger token1;

    public ProtocolFees (
        BigInteger token0, 
        BigInteger token1
    ) {
        this.token0 = token0;
        this.token1 = token1;
    }

    public static void writeObject(ObjectWriter w, ProtocolFees obj) {
        w.write(obj.token0);
        w.write(obj.token1);
    }

    public static ProtocolFees readObject(ObjectReader r) {
        return new ProtocolFees(
            r.readBigInteger(), // token0
            r.readBigInteger() // token1
        );
    }
}