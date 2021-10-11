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

package exchange.switchy.factory;

import exchange.switchy.utils.AddressUtils;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

/**
 * @title Canonical Switch factory
 * @notice Deploys Switch pools and manages ownership and control over pool protocol fees
 */
public class SwitchyFactory {

    // ================================================
    // Consts
    // ================================================
    
    // Contract class name
    private static final String NAME = "SwitchyFactory";

    // Contract name
    private final String name;

    // ================================================
    // DB Variables
    // ================================================
    private final VarDB<Address> owner = Context.newVarDB(NAME + "_owner", Address.class);
    private final DictDB<Integer, Integer> feeAmountTickSpacing = Context.newDictDB(NAME + "_feeAmountTickSpacing", Integer.class);
    private final BranchDB<Address, BranchDB<Address, DictDB<Integer, Address>>> getPool = Context.newBranchDB(NAME + "_getPool", Address.class);
    private final VarDB<byte[]> poolContract = Context.newVarDB(NAME + "_poolContract", byte[].class);

    // ================================================
    // Event Logs
    // ================================================
    /**
     * @notice Emitted when the owner of the factory is changed
     * @param oldOwner The owner before the owner was changed
     * @param newOwner The owner after the owner was changed
     */
    @EventLog(indexed = 2)
    protected void OwnerChanged(
        Address zeroAddress, 
        Address caller
    ) {}

    /**
     * @notice Emitted when a new fee amount is enabled for pool creation via the factory
     * @param fee The enabled fee, denominated in hundredths of a bip
     * @param tickSpacing The minimum number of ticks between initialized ticks for pools created with the given fee
     */
    @EventLog(indexed = 2)
    protected void FeeAmountEnabled(
        int fee, 
        int tickSpacing
    ) {}

    @EventLog(indexed = 3)
    protected void PoolCreated(
        Address token0, 
        Address token1, 
        int fee, 
        int tickSpacing, 
        Address pool
    ) {}

    // ================================================
    // Methods
    // ================================================
    /**
     *  Contract constructor
     *  
     */
    public SwitchyFactory() {
        final Address caller = Context.getCaller();
        this.name = "Switchy Factory";

        if (this.owner.get() == null) {
            this.owner.set(caller);
            this.OwnerChanged(AddressUtils.ZERO_ADDRESS, caller);
        }

        if (this.feeAmountTickSpacing.get(500) == null) {
            this.feeAmountTickSpacing.set(500, 10);
            this.FeeAmountEnabled(500, 10);
        }

        if (this.feeAmountTickSpacing.get(3000) == null) {
            this.feeAmountTickSpacing.set(3000, 60);
            this.FeeAmountEnabled(3000, 60);
        }

        if (this.feeAmountTickSpacing.get(10000) == null) {
            this.feeAmountTickSpacing.set(10000, 200);
            this.FeeAmountEnabled(10000, 200);
        }
    }

    /**
     * @notice Creates a pool for the given two tokens and fee
     * @param tokenA One of the two tokens in the desired pool
     * @param tokenB The other of the two tokens in the desired pool
     * @param fee The desired fee for the pool
     * @dev tokenA and tokenB may be passed in either order: token0/token1 or token1/token0. tickSpacing is retrieved
     * from the fee. The call will revert if the pool already exists, the fee is invalid, or the token arguments
     * are invalid.
     * @return pool The address of the newly created pool
     */
    @External
    public Address createPool(
        Address tokenA,
        Address tokenB,
        int fee,
        // TODO: UNPATCHME:
        Address pool
    ) {
        Context.require(!tokenA.equals(tokenB),
            "createPool: tokenA must be different from tokenB");

        Address token0 = tokenA;
        Address token1 = tokenB;

        if (AddressUtils.compareTo(tokenA, tokenB) >= 0) {
            token0 = tokenB;
            token1 = tokenA;
        }

        Context.require(!token0.equals(AddressUtils.ZERO_ADDRESS),
            "createPool: token0 cannot be ZERO_ADDRESS");

        int tickSpacing = this.feeAmountTickSpacing.getOrDefault(fee, 0);
        Context.require(tickSpacing != 0, 
            "createPool: tickSpacing cannot be 0");

        Context.require(getPool.at(token0).at(token1).get(fee) == null, 
            "createPool: pool already exists");

        /**  === TODO: FIX Begin patch === */
        // Address pool = SwitchyPoolDeployer.deploy(
        //     this.poolContract.get(), 
        //     Context.getAddress(), 
        //     token0, token1, fee, tickSpacing
        // );
        SwitchyPoolDeployer.parameters.set(new SwitchyPoolDeployerParameters(Context.getAddress(), token0, token1, fee, tickSpacing));
        /** ==== End Patch === */

        getPool.at(token0).at(token1).set(fee, pool);
        // populate mapping in the reverse direction, deliberate choice to avoid the cost of comparing addresses
        getPool.at(token1).at(token0).set(fee, pool);

        this.PoolCreated(token0, token1, fee, tickSpacing, pool);

        return pool;
    }

    /**
     * @notice Updates the owner of the factory
     * @dev Must be called by the current owner
     * @param _owner The new owner of the factory
     */
    @External
    public void setOwner (Address _owner) {
        checkOwner();

        Address currentOwner = this.owner.get();
        this.OwnerChanged(currentOwner, _owner);
        this.owner.set(_owner);
    }

    /**
     * @notice Enables a fee amount with the given tickSpacing
     * @dev Fee amounts may never be removed once enabled
     * @param fee The fee amount to enable, denominated in hundredths of a bip (i.e. 1e-6)
     * @param tickSpacing The spacing between ticks to be enforced for all pools created with the given fee amount
     */
    @External
    public void enableFeeAmount (int fee, int tickSpacing) {
        checkOwner();

        Context.require(fee < 1000000, 
            "enableFeeAmount; fee needs to be lower than 1000000");
        
        // tick spacing is capped at 16384 to prevent the situation where tickSpacing is so large that
        // TickBitmap#nextInitializedTickWithinOneWord overflows int24 container from a valid tick
        // 16384 ticks represents a >5x price change with ticks of 1 bips
        Context.require(tickSpacing > 0 && tickSpacing < 16384,
            "enableFeeAmount: tickSpacing > 0 && tickSpacing < 16384");
        
        Context.require(this.feeAmountTickSpacing.get(fee) == 0,
            "enableFeeAmount: fee amount is alreay enabled");

        this.feeAmountTickSpacing.set(fee, tickSpacing);
        this.FeeAmountEnabled(fee, tickSpacing);
    }

    @External
    public void setPoolContract (byte[] contractBytes) {
        checkOwner();

        this.poolContract.set(contractBytes);
    }

    // ================================================
    // Checks
    // ================================================
    private void checkOwner () {
        Address currentOwner = this.owner.get();
        Context.require(Context.getCaller().equals(currentOwner),
            "checkOwner: caller must be owner");
    }

    // ================================================
    // Public variable getters
    // ================================================
    @External(readonly = true)
    public String name() {
        return this.name;
    }

    @External(readonly = true)
    public byte[] poolContract () {
        return this.poolContract.get();
    }

    @External(readonly = true)
    public Address owner() {
        return this.owner.get();
    }

    @External(readonly = true)
    public Address getPool(Address token0, Address token1, int fee) {
        return this.getPool.at(token0).at(token1).get(fee);
    }
}
