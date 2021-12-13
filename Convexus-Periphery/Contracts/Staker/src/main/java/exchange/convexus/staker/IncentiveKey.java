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

package exchange.convexus.staker;

import java.math.BigInteger;

import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class IncentiveKey {

    // The token being distributed as a reward
    public Address rewardToken;
    // The Convexus pool
    public Address pool;
    // The time when the incentive program begins
    public BigInteger startTime;
    // The time when rewards stop accruing
    public BigInteger endTime;
    // The address which receives any remaining reward tokens when the incentive is ended
    public Address refundee;

    public IncentiveKey (
        Address rewardToken,
        Address pool,
        BigInteger startTime,
        BigInteger endTime,
        Address refundee
    ) {
        this.rewardToken = rewardToken; 
        this.pool = pool; 
        this.startTime = startTime; 
        this.endTime = endTime; 
        this.refundee = refundee; 
    }

    public static IncentiveKey readObject (ObjectReader r) {
        return new IncentiveKey(
            r.readAddress(), // rewardToken 
            r.readAddress(), // pool 
            r.readBigInteger(), // startTime 
            r.readBigInteger(), // endTime 
            r.readAddress() // refundee 
        );
    }
    public static void writeObject(ObjectWriter w, IncentiveKey obj) {
        w.write(obj.rewardToken);
        w.write(obj.pool);
        w.write(obj.startTime);
        w.write(obj.endTime);
        w.write(obj.refundee);
    }

    public String toString () {
        return "[KEY] \n rewardToken = " + rewardToken + " \n pool = " + pool + " (" + Context.call(pool, "name") + ")" + " \n startTime = " + startTime + " \n endTime = " + endTime + " \n refundee = " + refundee;
    }
}
