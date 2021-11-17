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

package exchange.convexus.governor;

import java.math.BigInteger;

import score.Address;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

/// @notice the ordered list of target addresses for calls to be made
class MethodCall {
  /// @notice The ordered list of values (i.e. msg.value) to be passed to the calls to be made
  public Address target;
  /// @notice The ordered list of function methods names to be called
  public BigInteger value;
  /// @notice The ordered list of params to be used for each call
  public String method;
  /// @notice The block at which voting begins: holders must delegate their votes prior to this block
  public Object[] params;

  public MethodCall (
    Address target,
    BigInteger value,
    String method,
    Object[] params
  ) {
    this.target = target;
    this.value = value;
    this.method = method;
    this.params = params;
  }

  public static MethodCall readObject(ObjectReader r) {
    
    var target = r.readAddress();
    var value = r.readBigInteger();
    var method = r.readString();
    var nParams = r.readInt();
    Object[] params = new Object[nParams];

    r.beginList();
    for (int i = 0; i < nParams; i++) {
      // params[i] = 
    }
    r.end();

    return new MethodCall(target, value, method, params);
  }

  public static void writeObject(ObjectWriter w, MethodCall obj) {
      // w.write(obj.fromBlock);
      // w.write(obj.votes);
  }
}