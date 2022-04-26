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

package exchange.convexus.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.reset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.iconloop.score.test.ServiceManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FactoryTest extends ConvexusFactoryTest {
  
  @BeforeEach
  void setup() throws Exception {
    ServiceManager.Block.resetInstance();
    setup_factory();
    setup_tokens();
    reset(factory.spy);
  }

  void setPoolContract () {
    try {
      byte[] rawBytes = Files.readAllBytes(Paths.get("../../../Contracts/Pool/build/libs/Pool-optimized.jar"));
      factory.invoke(owner, "setPoolContract", rawBytes);
    } catch (IOException e) {
      assertEquals(e.getMessage(), "");
    }
  }

  @Test
  void testSetContract() {
    setPoolContract();
  }

  @Test
  void testCreatePool () {
    setPoolContract();
    var poolAddress = ConvexusFactoryUtils.createPool(factory, owner, sicx.getAddress(), usdc.getAddress(), 500);
    assertNotEquals(poolAddress, null);
  }
}
