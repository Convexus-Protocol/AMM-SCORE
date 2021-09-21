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

package exchange.switchy.librairies;

import java.math.BigInteger;

import score.Context;

public class Position {
    public static class Info {
        // the amount of liquidity owned by this position
        public BigInteger liquidity;
        public BigInteger getliquidity() { return this.liquidity; }
        public void setliquidity(BigInteger v) { this.liquidity = v; }

        // fee growth per unit of liquidity as of the last update to liquidity or fees owed
        public BigInteger feeGrowthInside0LastX128;
        public BigInteger getfeeGrowthInside0LastX128() { return this.feeGrowthInside0LastX128; }
        public void setfeeGrowthInside0LastX128(BigInteger v) { this.feeGrowthInside0LastX128 = v; }

        public BigInteger feeGrowthInside1LastX128;
        public BigInteger getfeeGrowthInside1LastX128() { return this.feeGrowthInside1LastX128; }
        public void setfeeGrowthInside1LastX128(BigInteger v) { this.feeGrowthInside1LastX128 = v; }

        // the fees owed to the position owner in token0/token1
        public BigInteger tokensOwed0;
        public BigInteger gettokensOwed0() { return this.tokensOwed0; }
        public void settokensOwed0(BigInteger v) { this.tokensOwed0 = v; }

        public BigInteger tokensOwed1;
        public BigInteger gettokensOwed1() { return this.tokensOwed1; }
        public void settokensOwed1(BigInteger v) { this.tokensOwed1 = v; }
        
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
                    "Position::update: NP"); // disallow pokes for 0 liquidity positions
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
