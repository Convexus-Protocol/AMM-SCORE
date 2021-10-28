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

package exchange.convexus.librairies;

import java.math.BigInteger;

import score.Address;
import score.Context;

public class PeripheryPayments {

  /**
   * @param token The token to pay
   * @param recipient The entity that will receive payment
   * @param value The amount to pay
   */
  public static void pay(
    Address token,
    Address recipient,
    BigInteger value
  ) {
    // TODO: ICX payment
    Context.call(token, "transfer", recipient, value, "pay".getBytes());
  }
}
