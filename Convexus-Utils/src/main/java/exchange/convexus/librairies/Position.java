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

package exchange.convexus.librairies;

import java.math.BigInteger;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class Position {
    public static class Info {
        // the amount of liquidity owned by this position
        public BigInteger liquidity;

        // fee growth per unit of liquidity as of the last update to liquidity or fees owed
        public BigInteger feeGrowthInside0LastX128;

        public BigInteger feeGrowthInside1LastX128;

        // the fees owed to the position owner in token0/token1
        public BigInteger tokensOwed0;

        public BigInteger tokensOwed1;

        public Info(
            BigInteger liquidity ,
            BigInteger feeGrowthInside0LastX128, 
            BigInteger feeGrowthInside1LastX128,
            BigInteger tokensOwed0,
            BigInteger tokensOwed1
        ) {
            this.liquidity = liquidity;
            this.feeGrowthInside0LastX128 = feeGrowthInside0LastX128;
            this.feeGrowthInside1LastX128 = feeGrowthInside1LastX128;
            this.tokensOwed0 = tokensOwed0;
            this.tokensOwed1 = tokensOwed1;
        }

        public static void writeObject(ObjectWriter w, Info obj) {
            w.write(obj.liquidity);
            w.write(obj.feeGrowthInside0LastX128);
            w.write(obj.feeGrowthInside1LastX128);
            w.write(obj.tokensOwed0);
            w.write(obj.tokensOwed1);
        }

        public static Info readObject(ObjectReader r) {
            return new Info(
                r.readBigInteger(), // liquidity
                r.readBigInteger(), // feeGrowthInside0LastX128
                r.readBigInteger(), // feeGrowthInside1LastX128
                r.readBigInteger(), // tokensOwed0
                r.readBigInteger()  // tokensOwed1
            );
        }

        /**
         * @notice Credits accumulated fees to a user's position
         * @param self The individual position to update
         * @param liquidityDelta The change in pool liquidity as a result of the position update
         * @param feeGrowthInside0X128 The all-time fee growth in token0, per unit of liquidity, inside the position's tick boundaries
         * @param feeGrowthInside1X128 The all-time fee growth in token1, per unit of liquidity, inside the position's tick boundaries
         */
        public void update(
            BigInteger liquidityDelta, 
            BigInteger feeGrowthInside0X128,
            BigInteger feeGrowthInside1X128
        ) {
            BigInteger liquidityNext;
            
            if (liquidityDelta.equals(BigInteger.ZERO)) {
                Context.require(this.liquidity.compareTo(BigInteger.ZERO) > 0, 
                    "update: pokes aren't allowed for 0 liquidity positions"); // disallow pokes for 0 liquidity positions
                liquidityNext = this.liquidity;
            } else {
                liquidityNext = LiquidityMath.addDelta(this.liquidity, liquidityDelta);
            }

            BigInteger tokensOwed0 = FullMath.mulDiv(feeGrowthInside0X128.subtract(this.feeGrowthInside0LastX128), this.liquidity, FixedPoint128.Q128);
            BigInteger tokensOwed1 = FullMath.mulDiv(feeGrowthInside1X128.subtract(this.feeGrowthInside1LastX128), this.liquidity, FixedPoint128.Q128);

            // update the position
            if (!liquidityDelta.equals(BigInteger.ZERO)) {
                this.liquidity = liquidityNext;
            }

            this.feeGrowthInside0LastX128 = feeGrowthInside0X128;
            this.feeGrowthInside1LastX128 = feeGrowthInside1X128;

            if (tokensOwed0.compareTo(BigInteger.ZERO) > 0 || tokensOwed1.compareTo(BigInteger.ZERO) > 0) {
                // overflow is acceptable, have to withdraw before you hit type(uint128).max fees
                this.tokensOwed0 = this.tokensOwed0.add(tokensOwed0);
                this.tokensOwed1 = this.tokensOwed1.add(tokensOwed1);
            }
        }
    }
    
}
