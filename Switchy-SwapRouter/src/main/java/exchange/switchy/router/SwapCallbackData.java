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

package exchange.switchy.router;

import java.util.Arrays;

import exchange.switchy.utils.BytesUtils;
import score.Address;

class SwapCallbackData {
    public byte[] path;
    public Address payer;

    public SwapCallbackData (byte[] path, Address payer) {
        this.path = path;
        this.payer = payer;
    }

    public static SwapCallbackData fromBytes (byte[] data) {
        int size = data.length;
        byte[] path = Arrays.copyOfRange(data, 0, size - Address.LENGTH);
        Address payer = new Address(Arrays.copyOfRange(data, size - Address.LENGTH, size));
        return new SwapCallbackData(path, payer);
    }

    public byte[] toBytes () {
        return BytesUtils.concat(
            this.path,
            this.payer.toByteArray()
        );
    }
}