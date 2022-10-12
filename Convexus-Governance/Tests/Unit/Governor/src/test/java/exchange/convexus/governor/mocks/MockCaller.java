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

package exchange.convexus.governor.mocks;

import java.math.BigInteger;
import exchange.convexus.governor.MethodCall;
import exchange.convexus.governor.MethodParam;
import score.Address;
import score.annotation.External;

public class MockCaller {

  public MockCaller () {}
  
  @External(readonly = true)
  public Object call (Address target, String method, MethodParam[] params) {
    MethodCall methodcall = new MethodCall(target, BigInteger.ZERO, method, params);
    return methodcall.call();
  }
  
  @External
  public void invoke (Address target, BigInteger value, String method, MethodParam[] params) {
    MethodCall methodcall = new MethodCall(target, value, method, params);
    methodcall.call();
  }
}
