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

package exchange.convexus.utils;

import java.math.BigInteger;

import score.Address;

public class ArrayUtils {
    public static BigInteger[] arrayCopy (BigInteger[] array) {
        BigInteger[] result = new BigInteger[array.length];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }

    public static void fill (Object[] a, Object val) {
        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = val;
        }
    }
    public static void arrayFill (BigInteger[] array, BigInteger fill) {
        int size = array.length;
        for (int i = 0; i < size; i++) {
            array[i] = fill;
        }
    }

    public static void arrayFill (Address[] array, Address fill) {
        int size = array.length;
        for (int i = 0; i < size; i++) {
            array[i] = fill;
        }
    }

    public static BigInteger[] newFill (int size, BigInteger fill) {
        BigInteger[] result = new BigInteger[size];
        arrayFill(result, fill);
        return result;
    }

    public static Address[] newFill (int size, Address fill) {
        Address[] result = new Address[size];
        arrayFill(result, fill);
        return result;
    }

    public static String toString (Object[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i].toString());
            if (i != array.length - 1) sb.append(", ");
        }
        sb.append(']');
        return sb.toString();
    }

    public static boolean contains (Object[] array, Object item) {
        for (Object current : array) {
            if (current.equals(item)) {
                return true;
            }
        }
        return false;
    }
}
