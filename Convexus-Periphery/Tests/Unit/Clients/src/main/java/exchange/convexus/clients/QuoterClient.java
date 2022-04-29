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

package exchange.convexus.clients;

import exchange.convexus.periphery.quoter.QuoteExactInputSingleParams;
import exchange.convexus.periphery.quoter.QuoteResult;
import exchange.convexus.periphery.quoter.Quoter;
import exchange.convexus.utils.ScoreSpy;

public class QuoterClient {

  public static QuoteResult quoteExactInputSingle (
    ScoreSpy<Quoter> client,
    QuoteExactInputSingleParams params
  ) {
    return QuoteResult.fromMap(client.call("quoteExactInputSingle", params));
  }
}