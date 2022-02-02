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

package exchange.convexus.clients;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import com.iconloop.score.test.Account;

import org.mockito.ArgumentCaptor;

import exchange.convexus.quoter.QuoteExactInputSingleParams;
import exchange.convexus.quoter.QuoteResult;
import exchange.convexus.quoter.Quoter;
import exchange.convexus.utils.ScoreSpy;

public class QuoterClient {

  public static QuoteResult quoteExactInputSingle (
    ScoreSpy<Quoter> client,
    Account from,
    QuoteExactInputSingleParams params
  ) {
    reset(client.spy);
    client.invoke(from, "quoteExactInputSingle", params);

    // Get QuoteResult event
    ArgumentCaptor<BigInteger> amount = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<BigInteger> sqrtPriceX96After = ArgumentCaptor.forClass(BigInteger.class);
    ArgumentCaptor<Integer> initializedTicksCrossed = ArgumentCaptor.forClass(Integer.class);
    verify(client.spy).QuoteResult(amount.capture(), sqrtPriceX96After.capture(), initializedTicksCrossed.capture());
    return new QuoteResult(amount.getValue(), sqrtPriceX96After.getValue(), initializedTicksCrossed.getValue());
  }
}