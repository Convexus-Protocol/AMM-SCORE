/*
 * Copyright 2022 Convexus Protocol
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

package exchange.convexus.test.callee;

import java.math.BigInteger;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

public class FlashData {

  Address sender;
  BigInteger pay0;
  BigInteger pay1;

  public FlashData (
    Address sender, 
    BigInteger pay0, 
    BigInteger pay1
  ) {
    this.sender = sender;
    this.pay0 = pay0;
    this.pay1 = pay1;
  }

  public static void writeObject(ObjectWriter writer, FlashData flashData) {
    writer.write(flashData.sender);
    writer.write(flashData.pay0);
    writer.write(flashData.pay1);
  }

  public static FlashData readObject(ObjectReader reader) {
    return new FlashData(reader.readAddress(), reader.readBigInteger(), reader.readBigInteger());
  }
}
