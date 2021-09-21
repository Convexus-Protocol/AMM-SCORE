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

package exchange.switchy.common;

import score.Address;

public class SwitchyPoolDeployerParameters {
    public Address factory;
    public Address getfactory() { return this.factory; }
    public void setfactory(Address v) { this.factory = v; }

    public Address token0;
    public Address gettoken0() { return this.token0; }
    public void settoken0(Address v) { this.token0 = v; }

    public Address token1;
    public Address gettoken1() { return this.token1; }
    public void settoken1(Address v) { this.token1 = v; }

    public int fee;
    public int getfee() { return this.fee; }
    public void setfee(int v) { this.fee = v; }

    public int tickSpacing;
    public int gettickSpacing() { return this.tickSpacing; }
    public void settickSpacing(int v) { this.tickSpacing = v; }

    public SwitchyPoolDeployerParameters (
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
