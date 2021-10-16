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

package exchange.convexus.callee;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import scorex.io.Reader;
import scorex.io.StringReader;

public class ConvexusCallee {
  
    // ================================================
    // Consts
    // ================================================
    // Contract class name
    private static final String NAME = "ConvexusCallee";

  // ================================================
  // DB Variables
  // ================================================
  // User => Token => Amount
  private final BranchDB<Address, DictDB<Address, BigInteger>> deposited = Context.newBranchDB(NAME + "_deposited", BigInteger.class);

  @EventLog
  private void MintCallback(BigInteger amount0Owed, BigInteger amount1Owed) {}

  @External
  public void mint (
    Address pool,
    Address recipient,
    int tickLower,
    int tickUpper,
    BigInteger amount
  ) {
    Context.call(pool, "mint", recipient, tickLower, tickUpper, amount, Context.getAddress().toByteArray());
  }

  @External
  public void uniswapV3MintCallback (
    BigInteger amount0Owed,
    BigInteger amount1Owed,
    byte[] data
  ) {
    Address payer = new Address(data);
    final Address caller = Context.getCaller();

    this.MintCallback(amount0Owed, amount1Owed);

    if (amount0Owed.compareTo(ZERO) > 0) {
      pay(payer, (Address) Context.call(caller, "token0"), amount0Owed);
    }

    if (amount1Owed.compareTo(ZERO) > 0) {
      pay(payer, (Address) Context.call(caller, "token1"), amount1Owed);
    }
  }

  private void pay (Address payer, Address token, BigInteger owed) {
    final Address caller = Context.getCaller();
    checkEnoughDeposited(payer, token, owed);
    Context.call(token, "transfer", caller, owed);
  }

  // @External - this method is external through tokenFallback
  private void deposit (Address caller, Address tokenIn, BigInteger amountIn) {
    // --- Checks ---
    Context.require(amountIn.compareTo(ZERO) > 0, 
        "deposit: Deposit amount cannot be less or equal to 0");

    // --- OK from here ---
    var depositedUser = this.deposited.at(caller);
    BigInteger oldBalance = depositedUser.getOrDefault(tokenIn, ZERO);
    depositedUser.set(tokenIn, oldBalance.add(amountIn));
    Context.println("LM: deposit("+caller+")("+tokenIn+") = " + this.deposited.at(caller).get(tokenIn));
  }

  @External
  public void withdraw (Address token) {
    final Address caller = Context.getCaller();

    var depositedUser = this.deposited.at(caller);
    BigInteger amount = depositedUser.getOrDefault(caller, ZERO);

    if (amount.compareTo(ZERO) > 0) {
      Context.call(token, "transfer", caller, amount, "withdraw".getBytes());
      depositedUser.set(token, ZERO);
    }
  }

  @External
  public void tokenFallback (Address _from, BigInteger _value, @Optional byte[] _data) throws Exception {
      Reader reader = new StringReader(new String(_data));
      JsonValue input = Json.parse(reader);
      JsonObject root = input.asObject();
      String method = root.get("method").asString();
      Address token = Context.getCaller();

      switch (method)
      {
          case "deposit": {
              deposit(_from, token, _value);
              break;
          }

          default:
              Context.revert("tokenFallback: Unimplemented tokenFallback action");
      }
  }

  // ================================================
  // Checks
  // ================================================
  private void checkEnoughDeposited (Address address, Address token, BigInteger amount) {
    var depositedUser = this.deposited.at(address);
    Context.require(depositedUser.getOrDefault(token, ZERO).compareTo(amount) >= 0,
        "checkEnoughDeposited: user didn't deposit enough funds");
  }
}
