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

public class ExactInputSingleParams {
    // The token expected as output
    public Address tokenOut;
    // The pool fee
    public int fee;
    // The recipient address
    public Address recipient;
    // The timestamp deadline, in seconds
    public BigInteger deadline;
    // The minimum amount of token in output
    public BigInteger amountOutMinimum;
    // The Q64.96 sqrt price limit
    public BigInteger sqrtPriceLimitX96;

    public ExactInputSingleParams (
        Address tokenOut,
        int fee,
        Address recipient,
        BigInteger deadline,
        BigInteger amountOutMinimum,
        BigInteger sqrtPriceLimitX96
    ) {
        this.tokenOut = tokenOut;
        this.fee = fee;
        this.recipient = recipient;
        this.deadline = deadline;
        this.amountOutMinimum = amountOutMinimum;
        this.sqrtPriceLimitX96 = sqrtPriceLimitX96;
    }

    public static ExactInputSingleParams fromJson(JsonObject params) {
      return new ExactInputSingleParams(
          Address.fromString(params.get("tokenOut").asString()),
          params.get("fee").asInt(),
          Address.fromString(params.get("recipient").asString()),
          StringUtils.toBigInt(params.get("deadline").asString()),
          StringUtils.toBigInt(params.get("amountOutMinimum").asString()),
          StringUtils.toBigInt(params.get("sqrtPriceLimitX96").asString())
        );
    }

    public JsonObject toJson() {
        return Json.object()
            .add("tokenOut", tokenOut.toString())
            .add("fee", fee)
            .add("recipient", recipient.toString())
            .add("deadline", deadline.toString())
            .add("amountOutMinimum", amountOutMinimum.toString())
            .add("sqrtPriceLimitX96", sqrtPriceLimitX96.toString());
    }
}