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

package exchange.convexus.swappay;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import exchange.convexus.interfaces.irc2.IIRC2;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.utils.JSONUtils;
import score.Address;
import score.BranchDB;
import score.ByteArrayObjectWriter;
import score.Context;
import score.DictDB;
import score.ObjectReader;
import score.annotation.External;
import score.annotation.Optional;
import scorex.io.Reader;
import scorex.io.StringReader;

public class ConvexusSwapPay {

  // ================================================
  // Consts
  // ================================================
  // Contract class name
  private static final String NAME = "ConvexusSwapPay";

  // ================================================
  // DB Variables
  // ================================================
  // User => Token => Amount
  private final BranchDB<Address, DictDB<Address, BigInteger>> deposited = Context.newBranchDB(NAME + "_deposited", BigInteger.class);

  @External
  public void swap (
    Address pool,
    Address recipient, 
    boolean zeroForOne,
    BigInteger sqrtPriceX96, 
    BigInteger amountSpecified, 
    BigInteger pay0,
    BigInteger pay1
  ) {
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    writer.write(Context.getCaller().toByteArray());
    writer.write(pay0);
    writer.write(pay1);
    IConvexusPool.swap(pool, recipient, zeroForOne, amountSpecified, sqrtPriceX96, writer.toByteArray());
  }

  @External
  public void convexusSwapCallback (
    BigInteger amount0Delta,
    BigInteger amount1Delta,
    byte[] data
  ) {
    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", data);
    Address sender = reader.readAddress();
    BigInteger pay0 = reader.readBigInteger();
    BigInteger pay1 = reader.readBigInteger();
    final Address caller = Context.getCaller();

    if (pay0.compareTo(ZERO) > 0) {
      pay(sender, IConvexusPool.token0(caller), caller, pay0);
    } else if (pay1.compareTo(ZERO) > 0) {
      pay(sender, IConvexusPool.token1(caller), caller, pay1);
    }
  }

  private void pay (Address payer, Address token, Address destination, BigInteger owed) {
    checkEnoughDeposited(payer, token, owed);

    // Remove funds from deposited
    var depositedUser = this.deposited.at(payer);
    BigInteger oldBalance = depositedUser.getOrDefault(token, ZERO);
    depositedUser.set(token, oldBalance.subtract(owed));

    // Actually transfer the tokens
    IIRC2.transfer(token, destination, owed, JSONUtils.method("pay"));
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
  }

  @External
  public void withdraw (Address token) {
    final Address caller = Context.getCaller();

    var depositedUser = this.deposited.at(caller);
    BigInteger amount = depositedUser.getOrDefault(caller, ZERO);

    if (amount.compareTo(ZERO) > 0) {
      IIRC2.transfer(token, caller, amount, JSONUtils.method("withdraw"));
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
    BigInteger userBalance = depositedUser.getOrDefault(token, ZERO);
    // Context.println("[Callee][checkEnoughDeposited][" + IIRC2.symbol(token) + "] " + userBalance + " / " + amount);
    Context.require(userBalance.compareTo(amount) >= 0,
        // "checkEnoughDeposited: user didn't deposit enough funds - " + userBalance + "/" + amount);
        "checkEnoughDeposited: user didn't deposit enough funds");
  }
}
