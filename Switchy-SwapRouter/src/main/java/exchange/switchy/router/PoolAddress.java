package exchange.switchy.router;

import java.math.BigInteger;

import exchange.switchy.utils.AddressUtils;
import exchange.switchy.utils.BytesUtils;
import score.Address;
import score.Context;

class PoolKey {
  public Address token0;
  public Address token1;
  public int fee;

  public PoolKey(Address token0, Address token1, int fee) {
    this.token0 = token0;
    this.token1 = token1;
    this.fee = fee;
  }
}

public class PoolAddress {
  
  private final static byte[] POOL_INIT_CODE_HASH = new BigInteger("0xe34f199b19b2b4f47f68442619d555527d244f78a3297ea89325f843f87b8b54").toByteArray();

  /**
   * @notice Returns PoolKey: the ordered tokens with the matched fee levels
   * @param tokenA The first token of a pool, unsorted
   * @param tokenB The second token of a pool, unsorted
   * @param fee The fee level of the pool
   * @return Poolkey The pool details with ordered token0 and token1 assignments
   */
  public static PoolKey getPoolKey (Address tokenA, Address tokenB, int fee) {
    Address token0 = tokenA;
    Address token1 = tokenB;

    if (AddressUtils.compareTo(tokenA, tokenB) > 0) {
        token0 = tokenB;
        token1 = tokenA;
    }

    return new PoolKey(token0, token1, fee);
  }

  /**
   * @notice Deterministically computes the pool address given the factory and PoolKey
   * @param factory The Switchy factory contract address
   * @param key The PoolKey
   * @return pool The contract address of the pool
   */
  public static Address computeAddress (Address factory, PoolKey key) {
    Context.require(AddressUtils.compareTo(key.token0, key.token1) < 0,
      "computeAddress: key.token0 < key.token1");

    byte[] prefix = {(byte) 0xff};
    
    return new Address(
      Context.hash("keccak-256", BytesUtils.concat(
        prefix,
        factory.toByteArray(), 
        Context.hash("keccak-256", BytesUtils.concat(
            key.token0.toByteArray(),
            key.token1.toByteArray(),
            BytesUtils.intToBytes(key.fee)
        )),
        POOL_INIT_CODE_HASH
      ))
    );
  }

}
