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

package exchange.convexus.router;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import exchange.convexus.utils.StringUtils;
import score.Address;

public class ExactInputParams {
    // The `path` is a sequence of [`tokenAddress`, `fee`, `tokenAddress`], encoded in *reverse order*, which are the variables needed to compute each pool contract address in our sequence of swaps. The multihop swap router code will automatically find the correct pool with these variables, and execute the swap needed within each pool in our sequence.
    public byte[] path;
    // The destination address of the outbound asset
    public Address recipient;
    // The unix time after which a transaction will be reverted, to protect against long delays and the increased chance of large price swings therein
    public BigInteger deadline;
    // The maximum amount of token0 willing to be swapped for the specified amountOut of token1
    public BigInteger amountOutMinimum;
    
    public ExactInputParams (
      byte[] path,
      Address recipient,
      BigInteger deadline,
      BigInteger amountOutMinimum
    ) {
        this.path = path;
        this.recipient = recipient;
        this.deadline = deadline;
        this.amountOutMinimum = amountOutMinimum;
    }

    public static ExactInputParams fromJson(JsonObject params) {
      return new ExactInputParams(
          StringUtils.hexToByteArray(params.get("path").asString()),
          Address.fromString(params.get("recipient").asString()),
          StringUtils.toBigInt(params.get("deadline").asString()),
          StringUtils.toBigInt(params.get("amountOutMinimum").asString())
        );
    }

    public JsonObject toJson() {
        return Json.object()
            .add("path", StringUtils.byteArrayToHex(this.path))
            .add("recipient", this.recipient.toString())
            .add("deadline", this.deadline.toString())
            .add("amountOutMinimum", this.amountOutMinimum.toString());
    }
}