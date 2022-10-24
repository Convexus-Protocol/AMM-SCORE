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

import score.Context;
import score.VarDB;

public class ReentrancyLock {
    private final VarDB<Boolean> locked;

    public ReentrancyLock (String id) {
        locked = Context.newVarDB(id + "_locked", Boolean.class);
    }

    public Boolean get () {
        return this.locked.get();
    }

    /**
     * Enable or disable the reentrancy protection
     * @param state reentrancy protection state
     */
    public void lock (boolean state) {
        // Check current lock state
        boolean lock_state = this.locked.getOrDefault(false);
        Context.require(state != lock_state, "ReentrancyLock: wrong lock state: " + lock_state);

        // OK
        this.locked.set(state);
    }
}
