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
import score.ByteArrayObjectWriter;
import score.Context;
import score.DictDB;
import score.ObjectReader;
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

  @EventLog
  private void SwapCallback(BigInteger amount0Delta, BigInteger amount1Delta) {}

  @External
  public void mint (
    Address pool,
    Address recipient,
    int tickLower,
    int tickUpper,
    BigInteger amount
  ) {
    Context.call(pool, "mint", recipient, tickLower, tickUpper, amount, Context.getCaller().toByteArray());
  }

  @External
  public void convexusMintCallback (
    BigInteger amount0Owed,
    BigInteger amount1Owed,
    byte[] data
  ) {
    Address sender = new Address(data);
    final Address caller = Context.getCaller();

    this.MintCallback(amount0Owed, amount1Owed);

    if (amount0Owed.compareTo(ZERO) > 0) {
      pay(sender, (Address) Context.call(caller, "token0"), caller, amount0Owed);
    }
    
    if (amount1Owed.compareTo(ZERO) > 0) {
      pay(sender, (Address) Context.call(caller, "token1"), caller, amount1Owed);
    }
  }

  @External
  public void convexusSwapCallback (
    BigInteger amount0Delta,
    BigInteger amount1Delta,
    byte[] data
  ) {
    Address sender = new Address(data);
    final Address caller = Context.getCaller();

    this.SwapCallback(amount0Delta, amount1Delta);

    if (amount0Delta.compareTo(ZERO) > 0) {
      pay(sender, (Address) Context.call(caller, "token0"), caller, amount0Delta);
    } else if (amount1Delta.compareTo(ZERO) > 0) {
      pay(sender, (Address) Context.call(caller, "token1"), caller, amount1Delta);
    } else {
      // if both are not gt 0, both must be 0.
      Context.require(amount0Delta.equals(ZERO) && amount1Delta.equals(ZERO),
        "convexusSwapCallback: both amounts must be 0");
    }
  }

  private void pay (Address payer, Address token, Address destination, BigInteger owed) {
    checkEnoughDeposited(payer, token, owed);

    // Remove funds from deposited
    var depositedUser = this.deposited.at(payer);
    BigInteger oldBalance = depositedUser.getOrDefault(token, ZERO);
    depositedUser.set(token, oldBalance.subtract(owed));

    // Actually transfer the tokens
    Context.call(token, "transfer", destination, owed, "pay".getBytes());
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

  @External
  public void flash (
    Address pool,
    Address recipient,
    BigInteger amount0, 
    BigInteger amount1, 
    BigInteger pay0, 
    BigInteger pay1
  ) {
    ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
    FlashData.writeObject(writer, new FlashData(Context.getCaller(), pay0, pay1));
    Context.call(pool, "flash", recipient, amount0, amount1, writer.toByteArray());
  }

  @EventLog
  public void FlashCallback(BigInteger fee0, BigInteger fee1) {}

  @External
  public void convexusFlashCallback (
      BigInteger fee0,
      BigInteger fee1,
      byte[] data
  ) {
    this.FlashCallback(fee0, fee1);
    final Address caller = Context.getCaller();

    ObjectReader reader = Context.newByteArrayObjectReader("RLPn", data);
    FlashData flashData = FlashData.readObject(reader);

    Address sender = flashData.sender;
    BigInteger pay0 = flashData.pay0;
    BigInteger pay1 = flashData.pay1;

    if (pay0.compareTo(ZERO) > 0) {
      Address token0 = (Address) Context.call(caller, "token0");
      Context.println("[Callee][flashcallback] Paying " + pay0 + " " + Context.call(token0, "symbol") + " to the pool");
      pay(sender, token0, caller, pay0);
    }
    if (pay1.compareTo(ZERO) > 0) {
      Address token1 = (Address) Context.call(caller, "token1");
      Context.println("[Callee][flashcallback] Paying " + pay1 + " " + Context.call(token1, "symbol") + " to the pool");
      pay(sender, token1, caller, pay1);
    }
  }

  @External
  public void swapExact0For1 (Address pool, BigInteger amount0In, Address recipient, BigInteger sqrtPriceLimitX96) {
    Context.call(pool, "swap", recipient, true, amount0In, sqrtPriceLimitX96, Context.getCaller().toByteArray());
  }

  @External
  public void swapExact1For0 (Address pool, BigInteger amount1In, Address recipient, BigInteger sqrtPriceLimitX96) {
    Context.call(pool, "swap", recipient, false, amount1In, sqrtPriceLimitX96, Context.getCaller().toByteArray());
  }

  @External(readonly = true)
  public String name () {
    return new String(NAME);
  }

  // ================================================
  // Checks
  // ================================================
  private void checkEnoughDeposited (Address address, Address token, BigInteger amount) {
    var depositedUser = this.deposited.at(address);
    BigInteger userBalance = depositedUser.getOrDefault(token, ZERO);
    Context.println("[Callee][checkEnoughDeposited][" + Context.call(token, "symbol") + "] " + userBalance + " / " + amount);
    Context.require(userBalance.compareTo(amount) >= 0,
        // "checkEnoughDeposited: user didn't deposit enough funds - " + userBalance + "/" + amount);
        "checkEnoughDeposited: user didn't deposit enough funds");
  }
}
