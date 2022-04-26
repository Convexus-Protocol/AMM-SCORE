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

package exchange.convexus.callee;

import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import exchange.convexus.interfaces.irc2.IIRC2;
import exchange.convexus.pool.IConvexusPool;
import exchange.convexus.utils.IntUtils;
import exchange.convexus.utils.JSONUtils;
import exchange.convexus.utils.StringUtils;
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

public class ConvexusPoolCallee {
  // ================================================
  // Consts
  // ================================================
  // Contract class name
  private static final String NAME = "ConvexusPoolCallee";

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
    IConvexusPool.mint(pool, recipient, tickLower, tickUpper, amount, Context.getCaller().toByteArray());
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
      pay(sender, IConvexusPool.token0(caller), caller, amount0Owed);
    }
    
    if (amount1Owed.compareTo(ZERO) > 0) {
      pay(sender, IConvexusPool.token1(caller), caller, amount1Owed);
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
      pay(sender, IConvexusPool.token0(caller), caller, amount0Delta);
    } else if (amount1Delta.compareTo(ZERO) > 0) {
      pay(sender, IConvexusPool.token1(caller), caller, amount1Delta);
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

      case "swapExact0For1":
      case "swap1ForExact0":
      case "swap0ForExact1":
      case "swapExact1For0":
      {
        deposit(_from, token, _value);
        
        JsonObject params = root.get("params").asObject();
        Address pool = Address.fromString(params.get("pool").asString());
        Address recipient = Address.fromString(params.get("recipient").asString());
        BigInteger amount = _value;
        BigInteger sqrtPriceLimitX96 = StringUtils.toBigInt(params.get("sqrtPriceLimitX96").asString());

        switch (method) {
          case "swapExact0For1": swapExact0For1(pool, amount, recipient, sqrtPriceLimitX96, _from.toByteArray()); break;
          case "swap1ForExact0": swap1ForExact0(pool, amount, recipient, sqrtPriceLimitX96, _from.toByteArray()); break;
          case "swap0ForExact1": swap0ForExact1(pool, amount, recipient, sqrtPriceLimitX96, _from.toByteArray()); break; 
          case "swapExact1For0": swapExact1For0(pool, amount, recipient, sqrtPriceLimitX96, _from.toByteArray()); break;
        }
        break;
      }

      case "swapToLowerSqrtPrice":
      case "swapToHigherSqrtPrice":
      {
        deposit(_from, token, _value);
        
        JsonObject params = root.get("params").asObject();
        Address pool = Address.fromString(params.get("pool").asString());
        Address recipient = Address.fromString(params.get("recipient").asString());
        BigInteger sqrtPriceX96 = StringUtils.toBigInt(params.get("sqrtPriceX96").asString());

        switch (method) {
          case "swapToLowerSqrtPrice":  swapToLowerSqrtPrice (pool, sqrtPriceX96, recipient, _from.toByteArray()); break;
          case "swapToHigherSqrtPrice": swapToHigherSqrtPrice(pool, sqrtPriceX96, recipient, _from.toByteArray()); break;
        }
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
    writer.write(new FlashData(Context.getCaller(), pay0, pay1));
    IConvexusPool.flash(pool, recipient, amount0, amount1, writer.toByteArray());
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
    FlashData flashData = reader.read(FlashData.class);

    Address sender = flashData.sender;
    BigInteger pay0 = flashData.pay0;
    BigInteger pay1 = flashData.pay1;

    if (pay0.compareTo(ZERO) > 0) {
      Address token0 = IConvexusPool.token0(caller);
      Context.println("[Callee][flashcallback] Paying " + pay0 + " " + IIRC2.symbol(token0) + " to the pool");
      pay(sender, token0, caller, pay0);
    }
    if (pay1.compareTo(ZERO) > 0) {
      Address token1 = IConvexusPool.token1(caller);
      Context.println("[Callee][flashcallback] Paying " + pay1 + " " + IIRC2.symbol(token1) + " to the pool");
      pay(sender, token1, caller, pay1);
    }
  }

  private void swapExact0For1 (Address pool, BigInteger amount0In, Address recipient, BigInteger sqrtPriceLimitX96, byte[] data) {
    IConvexusPool.swap(pool, recipient, true, amount0In, sqrtPriceLimitX96, data);
  }

  private void swap1ForExact0 (Address pool, BigInteger amount0Out, Address recipient, BigInteger sqrtPriceLimitX96, byte[] data) {
    IConvexusPool.swap(pool, recipient, false, amount0Out.negate(), sqrtPriceLimitX96, data);
  }

  private void swap0ForExact1 (Address pool, BigInteger amount1Out, Address recipient, BigInteger sqrtPriceLimitX96, byte[] data) {
    IConvexusPool.swap(pool, recipient, true, amount1Out.negate(), sqrtPriceLimitX96, data);
  }

  private void swapExact1For0 (Address pool, BigInteger amount1In, Address recipient, BigInteger sqrtPriceLimitX96, byte[] data) {
    IConvexusPool.swap(pool, recipient, false, amount1In, sqrtPriceLimitX96, data);
  }

  private void swapToLowerSqrtPrice (Address pool, BigInteger sqrtPriceX96, Address recipient, byte[] data) {
    IConvexusPool.swap(pool, recipient, true, IntUtils.MAX_INT256, sqrtPriceX96, data);
  }

  private void swapToHigherSqrtPrice (Address pool, BigInteger sqrtPriceX96, Address recipient, byte[] data) {
    IConvexusPool.swap(pool, recipient, false, IntUtils.MAX_INT256, sqrtPriceX96, data);
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
    Context.println("[Callee][checkEnoughDeposited][" + IIRC2.symbol(token) + "][" + address + "] " + userBalance + " / " + amount);
    Context.require(userBalance.compareTo(amount) >= 0,
        // "checkEnoughDeposited: user didn't deposit enough funds - " + userBalance + "/" + amount);
        "checkEnoughDeposited: user didn't deposit enough funds");
  }
}
