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

package exchange.convexus.test.contracts.reentrantcallee;

import static exchange.convexus.utils.AddressUtils.ZERO_ADDRESS;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import exchange.convexus.interfaces.irc2.IIRC2ICX;
import exchange.convexus.librairies.TickMath;
import exchange.convexus.pool.IConvexusPool;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.annotation.External;
import score.annotation.Optional;
import scorex.io.Reader;
import scorex.io.StringReader;

public class ConvexusReentrantCallee {

  // ================================================
  // Consts
  // ================================================
  // Contract class name
  private static final String NAME = "ConvexusReentrantCallee";
  private static final String expectedReason = "ReentrancyLock: wrong lock state: true";

  // ================================================
  // DB Variables
  // ================================================
  // User => Token => Amount
  private final BranchDB<Address, DictDB<Address, BigInteger>> deposited = Context.newBranchDB(NAME + "_deposited", BigInteger.class);

  @External
  public void swapToReenter (Address pool) {
    IConvexusPool.swap(pool, ZERO_ADDRESS, false, ONE, TickMath.MAX_SQRT_RATIO.subtract(ONE), new byte[0]);
  }

  @External
  public void convexusSwapCallback (
    BigInteger amount0Delta,
    BigInteger amount1Delta,
    byte[] data
  ) {
    // try to reenter swap
    try {
      IConvexusPool.swap(Context.getCaller(), ZERO_ADDRESS, false, ONE, ZERO, new byte[0]);
    } catch (AssertionError e) {
      Context.require(e.getMessage().equals(expectedReason));
    }

    // try to reenter mint
    try {
      IConvexusPool.mint(Context.getCaller(), ZERO_ADDRESS, 0, 0, ZERO, new byte[0]);
    } catch (AssertionError e) {
      Context.require(e.getMessage().equals(expectedReason));
    }

    // try to reenter collect
    try {
      IConvexusPool.collect(Context.getCaller(), ZERO_ADDRESS, 0, 0, ZERO, ZERO);
    } catch (AssertionError e) {
      Context.require(e.getMessage().equals(expectedReason));
    }
    
    // try to reenter burn
    try {
      IConvexusPool.burn(Context.getCaller(), 0, 0, ZERO);
    } catch (AssertionError e) {
      Context.require(e.getMessage().equals(expectedReason));
    }
    
    // try to reenter flash
    try {
      IConvexusPool.flash(Context.getCaller(), ZERO_ADDRESS, ZERO, ZERO, new byte[0]);
    } catch (AssertionError e) {
      Context.require(e.getMessage().equals(expectedReason));
    }

    // try to reenter collectProtocol
    try {
      IConvexusPool.collectProtocol(Context.getCaller(), ZERO_ADDRESS, ZERO, ZERO);
    } catch (AssertionError e) {
      Context.require(e.getMessage().equals(expectedReason));
    }
    
    Context.require(false, "convexusSwapCallback: Unable to reenter");
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
      IIRC2ICX.transfer(token, caller, amount, "withdraw");
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
}
