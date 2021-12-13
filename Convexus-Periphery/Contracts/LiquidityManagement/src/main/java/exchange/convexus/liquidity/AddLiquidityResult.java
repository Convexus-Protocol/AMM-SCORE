package exchange.convexus.liquidity;

import java.math.BigInteger;

import score.Address;

public class AddLiquidityResult {
    public BigInteger liquidity;
    public BigInteger amount0;
    public BigInteger amount1;
    public Address pool;

    public AddLiquidityResult (
        BigInteger liquidity,
        BigInteger amount0,
        BigInteger amount1,
        Address pool
    ) {
        this.liquidity = liquidity;
        this.amount0 = amount0;
        this.amount1 = amount1;
        this.pool = pool;
    }
}