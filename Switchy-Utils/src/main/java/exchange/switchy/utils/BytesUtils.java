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

package exchange.switchy.utils;

import score.Context;

public class BytesUtils {
    public static int getBigEndianInt (byte[] bytearray) {
        Context.require(bytearray.length == 4, 
            "getBigEndianInt: Invalid bytearray size");
        return  ((bytearray[0] & 0xFF) << 24) 
              | ((bytearray[1] & 0xFF) << 16) 
              | ((bytearray[2] & 0xFF) << 8) 
              | ((bytearray[3] & 0xFF));
    }

    public static byte[] intToBytes (int data) {
        return new byte[] {
            (byte)((data >> 24) & 0xff),
            (byte)((data >> 16) & 0xff),
            (byte)((data >> 8) & 0xff),
            (byte)((data >> 0) & 0xff),
        };
    }

    public static byte[] concat (byte[] ... array) {
        int size = 0;

        for (byte[] item : array) {
            size += item.length;
        }

        byte[] result = new byte[size];
        int destPos = 0;

        for (byte[] item : array) {
            System.arraycopy(item, 0, result, destPos, item.length);
            destPos += item.length;
        }

        return result;
    }
}
