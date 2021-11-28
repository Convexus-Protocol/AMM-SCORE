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
import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import score.Address;

public class SwapRouterUtils {
  
  public static void exactInput (Account from, Score token, Address router, BigInteger _value, byte[] path, Address recipient, BigInteger deadline, BigInteger amountOutMinimum) {

    ExactInputParams params = new ExactInputParams(path, recipient, deadline, amountOutMinimum);

    JsonObject data = Json.object()
      .add("method", "exactInput")
      .add("params", params.toJson());

    byte[] dataBytes = data.toString().getBytes();

    token.invoke(
      from, 
      "transfer", 
      router, 
      _value, 
      dataBytes
    );
  }
  
  public static void exactInputSingle (
    Account from, Score tokenIn, Address router, BigInteger _value, 
    Address tokenOut, int fee, Address recipient, BigInteger deadline, BigInteger amountOutMinimum, BigInteger sqrtPriceLimitX96) {

    var params = new ExactInputSingleParams(
      tokenOut,
      fee,
      recipient,
      deadline,
      amountOutMinimum,
      sqrtPriceLimitX96
    );

    JsonObject data = Json.object()
      .add("method", "exactInputSingle")
      .add("params", params.toJson());

    byte[] dataBytes = data.toString().getBytes();

    tokenIn.invoke(
      from, 
      "transfer", 
      router, 
      _value, 
      dataBytes
    );
  }

  public static void exactOutput(
    Account from, 
    Score tokenIn, 
    Address router, 
    BigInteger _value,
    byte[] path,
    Address recipient,
    BigInteger deadline,
    BigInteger amountOut
  ) {
    var params = new ExactOutputParams(
      path,
      recipient,
      deadline,
      amountOut
    );

    JsonObject data = Json.object()
      .add("method", "exactOutput")
      .add("params", params.toJson());

    byte[] dataBytes = data.toString().getBytes();

    tokenIn.invoke(
      from, 
      "transfer", 
      router, 
      _value, 
      dataBytes
    );
  }

}
