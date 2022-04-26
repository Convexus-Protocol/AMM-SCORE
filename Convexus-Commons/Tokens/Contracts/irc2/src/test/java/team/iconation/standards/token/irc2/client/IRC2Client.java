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

package team.iconation.standards.token.irc2.client;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import score.Address;

public class IRC2Client {

  public static BigInteger balanceOf(Score irc2, Address account) {
    return (BigInteger) irc2.call("balanceOf", account);
  }
  public static BigInteger balanceOf(Score irc2, Account account) {
    return balanceOf(irc2, account.getAddress());
  }
  public static void transfer(Score irc2, Account from, Account to, BigInteger amount, byte[] data) {
    irc2.invoke(from, "transfer", to.getAddress(), amount, data);
  }
  public static void transfer(Score irc2, Account from, Account to, BigInteger amount) {
    transfer(irc2, from, to, amount, "".getBytes());
  }
}