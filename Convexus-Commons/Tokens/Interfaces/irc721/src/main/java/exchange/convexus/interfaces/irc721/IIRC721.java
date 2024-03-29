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

package exchange.convexus.interfaces.irc721;

import java.math.BigInteger;

import score.Address;
import score.Context;

public class IIRC721 {

  public static void safeTransferFrom (
    Address irc721,
    Address from,
    Address to,
    BigInteger tokenId,
    byte[] data
  ) {
    Context.call(irc721, "safeTransferFrom", from, to, tokenId, data);
  }

  public static void safeTransferFrom (
    Address irc721,
    Address from,
    Address to,
    BigInteger tokenId
  ) {
    Context.call(irc721, "safeTransferFrom", from, to, tokenId);
  }

  public static void transferFrom (
    Address irc721,
    Address from,
    Address to,
    BigInteger tokenId
  ) {
    Context.call(irc721, "transferFrom", from, to, tokenId);
  }

  public static Address ownerOf (Address irc721, BigInteger tokenId) {
    return (Address) Context.call (irc721, "ownerOf", tokenId);
  }
}
