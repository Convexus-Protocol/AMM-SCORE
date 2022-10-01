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

package exchange.convexus.governor;

import java.math.BigInteger;

import score.Address;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

/// @notice the ordered list of target addresses for calls to be made
public class MethodCall {
  /// @notice target addresses for calls to be made
  public Address target;
  /// @notice value (i.e. ICX) to be passed to the call to be made
  public BigInteger value;
  /// @notice function method name to be called
  public String method;
  /// @notice params to be used
  public MethodParam[] params;

  public MethodCall () {}

  public MethodCall (
    Address target,
    BigInteger value,
    String method,
    MethodParam[] params
  ) {
    this.target = target;
    this.value = value;
    this.method = method;
    this.params = params;
  }

  public MethodCall (
    Address target,
    String method,
    MethodParam[] params
  ) {
    this.target = target;
    this.value = BigInteger.ZERO;
    this.method = method;
    this.params = params;
  }

  public static MethodCall readObject (ObjectReader r) {
    var target = r.readAddress();
    var value = r.readBigInteger();
    var method = r.readString();
    var nParams = r.readInt();
    MethodParam[] params = new MethodParam[nParams];

    r.beginList();
    for (int i = 0; i < nParams; i++) {
      params[i] = r.read(MethodParam.class);
    }
    r.end();

    return new MethodCall (target, value, method, params);
  }

  public static void writeObject (ObjectWriter w, MethodCall obj) {
    int nParams = obj.params.length;
    w.write(obj.target);
    w.write(obj.value);
    w.write(obj.method);
    w.write(nParams);
    w.beginList(nParams);
    for (int i = 0; i < nParams; i++) {
      w.write(obj.params[i]);
    }
    w.end();
  }

  public byte[] toBytes () {
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    writer.write(this);
    return writer.toByteArray();
  }

  public Object call () {
    Object[] convertedParams = new Object[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      convertedParams[i] = this.params[i].convert();
    }
    return Context.call(this.value, this.target, this.method, convertedParams);
  }
}