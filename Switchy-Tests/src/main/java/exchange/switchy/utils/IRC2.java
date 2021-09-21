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

package exchange.switchy.utils;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

public class IRC2 {
  public static BigInteger balanceOf (Score token, Account account) {
    return (BigInteger) token.call("balanceOf", account.getAddress());
  }

  public static BigInteger totalSupply (Score token) {
    return (BigInteger) token.call("totalSupply");
  }
}
