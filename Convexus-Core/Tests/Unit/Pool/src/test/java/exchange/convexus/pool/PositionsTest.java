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

package exchange.convexus.pool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exchange.convexus.core.pool.contracts.models.Positions;
import exchange.convexus.utils.StringUtils;
import score.Address;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PositionsTest extends ConvexusPoolTest {

  @BeforeEach
  void setup() throws Exception {
  }

  @Test
  void testGetKey () {
    assertEquals("A8F446F2BCC0799EB6B025C457E4638F11BBFAA62180D4B64BE3E72B6CF1D54C", 
      StringUtils.byteArrayToHex(
        Positions.getKey(
          Address.fromString("cx0000000000000000000000000000000000000001"), 
          123, 456
        )
      )
    );
  }
}