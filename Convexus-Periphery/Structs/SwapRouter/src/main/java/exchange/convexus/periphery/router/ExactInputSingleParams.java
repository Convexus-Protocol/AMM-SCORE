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
import exchange.convexus.interfaces.irc2.IRC2ICXParam;
import exchange.convexus.utils.StringUtils;
import score.Address;

public class ExactInputSingleParams implements IRC2ICXParam {
    // The contract address of the outbound token
    public Address tokenOut;
    // The fee tier of the pool, used to determine the correct pool contract in which to execute the swap
    public int fee;
    // The destination address of the outbound token
    public Address recipient;
    // The unix time after which a swap will fail, to protect against long-pending transactions and wild swings in prices
    public BigInteger deadline;
    // The minimum amount of token in output
    public BigInteger amountOutMinimum;
    // The Q64.96 sqrt price limit
    public BigInteger sqrtPriceLimitX96;

    public ExactInputSingleParams () {}

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
          StringUtils.toBigInt(params.get("fee").asString()).intValue(),
          Address.fromString(params.get("recipient").asString()),
          StringUtils.toBigInt(params.get("deadline").asString()),
          StringUtils.toBigInt(params.get("amountOutMinimum").asString()),
          StringUtils.toBigInt(params.get("sqrtPriceLimitX96").asString())
        );
    }

    public JsonObject toJson() {
        return Json.object()
            .add("tokenOut", tokenOut.toString())
            .add("fee", Integer.toString(fee))
            .add("recipient", recipient.toString())
            .add("deadline", deadline.toString())
            .add("amountOutMinimum", amountOutMinimum.toString())
            .add("sqrtPriceLimitX96", sqrtPriceLimitX96.toString());
    }

    @Override
    public Object[] toRaw() {
        return new Object[] {
            this.tokenOut,
            this.fee,
            this.recipient,
            this.deadline,
            this.amountOutMinimum,
            this.sqrtPriceLimitX96,
        };
    }
}