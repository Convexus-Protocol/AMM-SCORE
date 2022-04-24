package exchange.convexus.pairflash;

import java.math.BigInteger;

import exchange.convexus.pool.PoolAddress.PoolKey;
import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

// fee2 and fee3 are the two other fees associated with the two other pools of token0 and token1
public class FlashCallbackData {
    BigInteger amount0;
    BigInteger amount1;
    Address payer;
    PoolKey poolKey;
    int poolFee2;
    int poolFee3;

    public FlashCallbackData (
        BigInteger amount0,
        BigInteger amount1,
        Address payer,
        PoolKey poolKey,
        int poolFee2,
        int poolFee3
    ) {
        this.amount0 = amount0;
        this.amount1 = amount1;
        this.payer = payer;
        this.poolKey = poolKey;
        this.poolFee2 = poolFee2;
        this.poolFee3 = poolFee3;
    }

    public static FlashCallbackData readObject(ObjectReader reader) {
        BigInteger amount0 = reader.readBigInteger();
        BigInteger amount1 = reader.readBigInteger();
        Address payer = reader.readAddress();
        PoolKey poolKey = reader.read(PoolKey.class);
        int poolFee2 = reader.readInt();
        int poolFee3 = reader.readInt();
        return new FlashCallbackData(amount0, amount1, payer, poolKey, poolFee2, poolFee3);
    }

    public static void writeObject(ObjectWriter w, FlashCallbackData obj) {
      w.write(obj.amount0);
      w.write(obj.amount1);
      w.write(obj.payer);
      w.write(obj.poolKey);
      w.write(obj.poolFee2);
      w.write(obj.poolFee3);
    }
}