/*
 * Copyright 2020 ICONLOOP Inc.
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

package team.iconation.standards.token.irc2;

import score.Context;
import score.annotation.External;

import java.math.BigInteger;

public class IRC2Burnable extends IRC2Basic {
    public IRC2Burnable(String _name, String _symbol, int _decimals, BigInteger _initialSupply) {
        super(_name, _symbol, _decimals, _initialSupply);
    }

    /**
     * Destroys `_amount` tokens from the caller.
     */
    @External
    public void burn(BigInteger _amount) {
        _burn(Context.getCaller(), _amount);
    }
}
