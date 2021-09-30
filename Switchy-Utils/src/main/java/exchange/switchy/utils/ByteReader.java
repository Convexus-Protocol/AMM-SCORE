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

import java.math.BigInteger;
import java.util.Arrays;

import score.Address;

public class ByteReader {

  private int offset;
  private final byte[] data;

  public ByteReader (byte [] data) {
    this.offset = 0;
    this.data = data;
  }

  public byte[] read (int size) {
    byte[] result = Arrays.copyOfRange(data, this.offset, this.offset + size);
    this.offset += size;
    return result;
  }

  public int readInt () {
    return BytesUtils.getBigEndianInt(this.read(BytesUtils.INT_SIZE));
  }

  public BigInteger readBigInteger (int size) {
    return new BigInteger(this.read(size));
  }

  public Address readAddress() {
    return new Address(this.read(Address.LENGTH));
  }

  public int remainingBytes() {
    return this.data.length - this.offset;
  }
}
