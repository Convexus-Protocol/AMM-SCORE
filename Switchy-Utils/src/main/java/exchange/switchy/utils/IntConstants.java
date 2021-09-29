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

public class IntConstants {
  public final static BigInteger MAX_UINT8   = new BigInteger("ff", 16);
  public final static BigInteger MAX_UINT16  = new BigInteger("ffff", 16);
  public final static BigInteger MAX_UINT32  = new BigInteger("ffffffff", 16);
  public final static BigInteger MAX_UINT64  = new BigInteger("ffffffffffffffff", 16);
  public final static BigInteger MAX_UINT128 = new BigInteger("ffffffffffffffffffffffffffffffff", 16);
  // TODO: FixMe
  public final static BigInteger MAX_UINT256 = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
}
