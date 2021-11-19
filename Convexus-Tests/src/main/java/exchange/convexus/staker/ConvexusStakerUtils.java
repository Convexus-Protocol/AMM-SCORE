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

package exchange.convexus.staker;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import score.Address;

public class ConvexusStakerUtils {
  
  public static void createIncentive (Account from, Score token, BigInteger amount, Address staker, Address pool, BigInteger startTime, BigInteger endTime, Address refundee) {

    var params = Json.object()
      .add("pool", pool.toString())
      .add("startTime", startTime.toString())
      .add("endTime", endTime.toString())
      .add("refundee", refundee.toString());

    JsonObject data = Json.object()
      .add("method", "createIncentive")
      .add("params", params);

    byte[] dataBytes = data.toString().getBytes();

    token.invoke(
      from, 
      "transfer", 
      staker,
      amount, 
      dataBytes
    );
  }
}
