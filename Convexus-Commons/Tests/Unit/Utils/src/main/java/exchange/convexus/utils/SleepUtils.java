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

package exchange.convexus.utils;

import static exchange.convexus.utils.TimeUtils.now;

import java.math.BigInteger;

import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

public class SleepUtils extends TestBase {
  protected static final ServiceManager sm = getServiceManager();
  
  public static void sleep (long seconds) {
    // 1 secs block generation
    sm.getBlock().increase(seconds);
  }

  public static void sleep (BigInteger seconds) {
    sleep(seconds.longValue());
  }

  public static void sleepTo (BigInteger timestampSeconds) {
    sleep(timestampSeconds.subtract(now()).longValue());
  }
}
