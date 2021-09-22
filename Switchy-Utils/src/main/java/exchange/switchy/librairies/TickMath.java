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

package exchange.switchy.librairies;

import static exchange.switchy.utils.MathUtils.gt;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

import exchange.switchy.utils.IntConstants;
import score.Context;

// @title Math library for computing sqrt prices from ticks and vice versa
// @notice Computes sqrt price for ticks of size 1.0001, i.e. sqrt(1.0001^tick) as fixed point Q64.96 numbers. Supports
// prices between 2**-128 and 2**128
public class TickMath {
  // @dev The minimum tick that may be passed to #getSqrtRatioAtTick computed from log base 1.0001 of 2**-128
  public final static int MIN_TICK = -887272;
  // @dev The maximum tick that may be passed to #getSqrtRatioAtTick computed from log base 1.0001 of 2**128
  public final static int MAX_TICK = -MIN_TICK;
  
  // @dev The minimum value that can be returned from #getSqrtRatioAtTick. Equivalent to getSqrtRatioAtTick(MIN_TICK)
  public final static BigInteger MIN_SQRT_RATIO = new BigInteger("4295128739");
  // @dev The maximum value that can be returned from #getSqrtRatioAtTick. Equivalent to getSqrtRatioAtTick(MAX_TICK)
  public final static BigInteger MAX_SQRT_RATIO = new BigInteger("1461446703485210103287273052203988822378723970342");

  public static BigInteger getSqrtRatioAtTick (int tick) {
    BigInteger absTick = tick < 0 ? BigInteger.valueOf(-tick) : BigInteger.valueOf(tick);
    Context.require(absTick.compareTo(BigInteger.valueOf(MAX_TICK)) <= 0, 
      "getSqrtRatioAtTick: tick can't be superior to MAX_TICK");

    BigInteger ratio = !absTick.and(BigInteger.valueOf(0x1)).equals(ZERO) ? new BigInteger("0xfffcb933bd6fad37aa2d162d1a594001") : new BigInteger("0x100000000000000000000000000000000");
    if (!absTick.and(BigInteger.valueOf(0x2)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xfff97272373d413259a46990580e213a"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x4)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xfff2e50f5f656932ef12357cf3c7fdcc"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x8)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xffe5caca7e10e4e61c3624eaa0941cd0"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x10)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xffcb9843d60f6159c9db58835c926644"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x20)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xff973b41fa98c081472e6896dfb254c0"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x40)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xff2ea16466c96a3843ec78b326b52861"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x80)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xfe5dee046a99a2a811c461f1969c3053"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x100)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xfcbe86c7900a88aedcffc83b479aa3a4"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x200)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xf987a7253ac413176f2b074cf7815e54"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x400)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xf3392b0822b70005940c7a398e4b70f3"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x800)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xe7159475a2c29b7443b29c7fa6e889d9"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x1000)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xd097f3bdfd2022b8845ad8f792aa5825"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x2000)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0xa9f746462d870fdf8a65dc1f90e061e5"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x4000)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0x70d869a156d2a1b890bb3df62baf32f7"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x8000)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0x31be135f97d08fd981231505542fcfa6"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x10000)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0x9aa508b5b7a84e1c677de54f3e99bc9"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x20000)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0x5d6af8dedb81196699c329225ee604"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x40000)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0x2216e584f5fa1ea926041bedfe98"))).shiftRight(128);
    if (!absTick.and(BigInteger.valueOf(0x80000)).equals(ZERO)) ratio = (ratio.multiply(new BigInteger("0x48a170391f7dc42444e8fa2"))).shiftRight(128);

    if (tick > 0) {
      ratio = IntConstants.MAX_UINT256.divide(ratio);
    }

    // this divides by 1<<32 rounding up to go from a Q128.128 to a Q128.96.
    // we then downcast because we know the result always fits within 160 bits due to our tick input constraint
    // we round up in the division so getTickAtSqrtRatio of the output price is always consistent
    return (ratio.shiftRight(32).add(ratio.mod(ONE.shiftLeft(32)).equals(ZERO) ? ZERO : ONE));
  }  

  public static int getTickAtSqrtRatio (BigInteger sqrtPriceX96) {
      // second inequality must be < because the price can never reach the price at the max tick
      Context.require(sqrtPriceX96.compareTo(MIN_SQRT_RATIO) >= 0 
                   && sqrtPriceX96.compareTo(MAX_SQRT_RATIO) < 0, 
        "getTickAtSqrtRatio: preconditions failed");
      
      BigInteger ratio = sqrtPriceX96.shiftLeft(32);
      BigInteger r = ratio;
      BigInteger msb = ZERO;
      BigInteger f = null;

      f = gt(r, new BigInteger("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")).shiftLeft(7);
      msb = msb.or(f);
      r = r.shiftRight(f.intValue());
      
      f = gt(r, new BigInteger("0xFFFFFFFFFFFFFFFF")).shiftLeft(6);
      msb = msb.or(f);
      r = r.shiftRight(f.intValue());

      f = gt(r, new BigInteger("0xFFFFFFFF")).shiftLeft(5);
      msb = msb.or(f);
      r = r.shiftRight(f.intValue());

      f = gt(r, new BigInteger("0xFFFF")).shiftLeft(4);
      msb = msb.or(f);
      r = r.shiftRight(f.intValue());
      
      f = gt(r, new BigInteger("0xFF")).shiftLeft(3);
      msb = msb.or(f);
      r = r.shiftRight(f.intValue());
      
      f = gt(r, new BigInteger("0xF")).shiftLeft(2);
      msb = msb.or(f);
      r = r.shiftRight(f.intValue());
      
      f = gt(r, new BigInteger("0x3")).shiftLeft(1);
      msb = msb.or(f);
      r = r.shiftRight(f.intValue());
      
      f = gt(r, new BigInteger("0x1"));
      msb = msb.or(f);

      if (msb.compareTo(BigInteger.valueOf(128)) >= 0) {
        r = ratio.shiftRight(msb.intValue() - 127);
      } else {
        r = ratio.shiftLeft(127 - msb.intValue());
      }

      BigInteger log_2 = msb.subtract(BigInteger.valueOf(128)).shiftLeft(64);

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(63));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(62));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(61));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(60));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(59));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(58));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(57));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(56));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(55));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(54));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(53));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(52));
      r = r.shiftRight(f.intValue());

      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(51));
      r = r.shiftRight(f.intValue());
      
      r = r.multiply(r).shiftRight(127);
      f = r.shiftRight(128);
      log_2 = log_2.or(f.shiftLeft(50));

      BigInteger log_sqrt10001 = log_2.multiply(new BigInteger("255738958999603826347141")); // 128.128 number
      int tickLow = log_sqrt10001.subtract(new BigInteger("3402992956809132418596140100660247210")).shiftRight(128).intValue();
      int tickHi = log_sqrt10001.add(new BigInteger("291339464771989622907027621153398088495")).shiftRight(128).intValue();

      return tickLow == tickHi ? tickLow : getSqrtRatioAtTick(tickHi).compareTo(sqrtPriceX96) <= 0 ? tickHi : tickLow;
  }
}
