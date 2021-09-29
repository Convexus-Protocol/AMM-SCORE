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

package exchange.switchy.initializer;

import score.annotation.External;

/**
 * @title Creates and initializes V3 Pools
 */
public class SwitchyPoolInitializer {

    // ================================================
    // Consts
    // ================================================
    // Contract class name
    // private static final String NAME = "SwitchyPoolInitializer";

    // Contract name
    private final String name;

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     *  
     */
    public SwitchyPoolInitializer() {
        // final Address caller = Context.getCaller();
        this.name = "Switchy Pool Initializer";
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly=true)
    public String name() {
        return this.name;
    }
}
