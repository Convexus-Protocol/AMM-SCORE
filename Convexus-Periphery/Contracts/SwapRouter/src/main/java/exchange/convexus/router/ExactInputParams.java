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

package exchange.convexus.router;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import exchange.convexus.utils.StringUtils;
import score.Address;

public class ExactInputParams {
    byte[] path;
    Address recipient;
    BigInteger deadline;
    BigInteger amountOutMinimum;
    
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