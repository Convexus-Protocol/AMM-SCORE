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

package exchange.convexus.periphery.router;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import exchange.convexus.interfaces.irc2.Parameterizable;
import exchange.convexus.utils.StringUtils;
import score.Address;

public class ExactOutputParams implements Parameterizable {
    public byte[] path;
    public Address recipient;
    public BigInteger deadline;
    public BigInteger amountOut;
    
    public ExactOutputParams (
      byte[] path,
      Address recipient,
      BigInteger deadline,
      BigInteger amountOut
    ) {
        this.path = path;
        this.recipient = recipient;
        this.deadline = deadline;
        this.amountOut = amountOut;
    }

    public static ExactOutputParams fromJson(JsonObject params) {
      return new ExactOutputParams(
          StringUtils.hexToByteArray(params.get("path").asString()),
          Address.fromString(params.get("recipient").asString()),
          StringUtils.toBigInt(params.get("deadline").asString()),
          StringUtils.toBigInt(params.get("amountOut").asString())
        );
    }

    public JsonObject toJson() {
        return Json.object()
            .add("path", StringUtils.byteArrayToHex(this.path))
            .add("recipient", this.recipient.toString())
            .add("deadline", this.deadline.toString())
            .add("amountOut", this.amountOut.toString());
    }

    @Override
    public Object[] toRaw() {
      return new Object[] {
        this.path,
        this.recipient,
        this.deadline,
        this.amountOut
      };
    }
}