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

package exchange.convexus.timelock;

import java.math.BigInteger;
import exchange.convexus.governor.MethodCall;
import score.Address;
import score.Context;

public class ITimelock {
  public static BigInteger delay (Address timelock) {
    return (BigInteger) Context.call(timelock, "delay");
  }

  public static boolean queuedTransactions (Address timelock, byte[] hash) {
    return (boolean) Context.call(timelock, "queuedTransactions", hash);
  }

  public static void queueTransaction (Address timelock, MethodCall call, BigInteger eta) {
    Context.call(timelock, "queueTransaction", call, eta);
  }

  public static Object executeTransaction (Address timelock, MethodCall call, BigInteger eta) {
    return Context.call(timelock, "executeTransaction", call, eta);
  }

  public static void cancelTransaction(Address timelock, MethodCall call, BigInteger eta) {
    Context.call(timelock, "cancelTransaction", call, eta);
  }

  public static BigInteger GRACE_PERIOD(Address timelock) {
    return (BigInteger) Context.call(timelock, "GRACE_PERIOD");
  }
}
