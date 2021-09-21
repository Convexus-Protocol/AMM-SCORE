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

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;

import score.Address;

public class ScoreSpy<T> {
  public Score score;
  public T spy;

  public ScoreSpy (Score score, T spy) {
      this.score = score;
      this.spy = spy;
  }

  public Object call(String method, Object... params) {
    return this.score.call(method, params);
  }

  public void invoke(Account from, String method, Object... params) {
    this.score.invoke(from, method, params);
  }

  public Address getAddress () {
    return this.score.getAddress();
  }

  public Account getAccount () {
    return this.score.getAccount();
  }
}
