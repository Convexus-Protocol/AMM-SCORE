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

package exchange.switchy.liquidity;

import exchange.switchy.librairies.PoolAddress;
import exchange.switchy.utils.ByteReader;
import exchange.switchy.utils.BytesUtils;
import score.Address;

public class MintCallbackData {
    PoolAddress.PoolKey poolKey;
    Address payer;

    public MintCallbackData (PoolAddress.PoolKey poolKey, Address payer) {
        this.poolKey = poolKey;
        this.payer = payer;
    }

    public static MintCallbackData fromBytes(ByteReader reader) {
        PoolAddress.PoolKey poolKey = PoolAddress.PoolKey.fromBytes(reader);
        Address payer = reader.readAddress();
        return new MintCallbackData(poolKey, payer);
    }

    public byte[] toBytes () {
        return BytesUtils.concat(
            this.poolKey.toBytes(),
            this.payer.toByteArray()
        );
    }
}