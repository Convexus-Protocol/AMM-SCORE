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

package exchange.convexus.periphery.cxs;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import score.ObjectReader;
import score.ObjectWriter;

/// A checkpoint for marking number of votes from a given block
public class Checkpoint {
    public long fromBlock;
    public BigInteger votes;

    public Checkpoint () {
      this.fromBlock = 0; 
      this.votes = ZERO;
    }

    public Checkpoint (long fromBlock, BigInteger votes) {
        this.fromBlock = fromBlock;
        this.votes = votes;
    }

    public static Checkpoint readObject(ObjectReader r) {
        return new Checkpoint(
            r.readLong(), // fromBlock 
            r.readBigInteger() // votes
        );
    }

    public static void writeObject(ObjectWriter w, Checkpoint obj) {
        w.write(obj.fromBlock);
        w.write(obj.votes);
    }
}