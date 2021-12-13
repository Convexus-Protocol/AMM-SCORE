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

public class ExactOutputSingleParams {
  public ExactOutputSingleParams(
    Address tokenOut, 
    int fee, 
    Address recipient, 
    BigInteger deadline,
    BigInteger amountOut,
    BigInteger sqrtPriceLimitX96
  ) {
    this.tokenOut = tokenOut;
    this.fee = fee;
    this.recipient = recipient;
    this.deadline = deadline;
    this.amountOut = amountOut;
    this.sqrtPriceLimitX96 = sqrtPriceLimitX96;
  }

  public Address tokenOut;
  public int fee;
  public Address recipient;
  public BigInteger deadline;
  public BigInteger amountOut;
  public BigInteger sqrtPriceLimitX96;
  
  public JsonObject toJson() {
    return Json.object()
        .add("tokenOut", tokenOut.toString())
        .add("fee", fee)
        .add("recipient", recipient.toString())
        .add("deadline", deadline.toString())
        .add("amountOut", amountOut.toString())
        .add("sqrtPriceLimitX96", sqrtPriceLimitX96.toString());
}

  public static ExactOutputSingleParams fromJson(JsonObject params) {
    return new ExactOutputSingleParams(
        Address.fromString(params.get("tokenOut").asString()),
        params.get("fee").asInt(),
        Address.fromString(params.get("recipient").asString()),
        StringUtils.toBigInt(params.get("deadline").asString()),
        StringUtils.toBigInt(params.get("amountOut").asString()),
        StringUtils.toBigInt(params.get("sqrtPriceLimitX96").asString())
      );
  }
}
