package exchange.switchy.pairflash;

import java.math.BigInteger;

import exchange.switchy.librairies.PoolAddress;
import exchange.switchy.utils.ByteReader;
import exchange.switchy.utils.BytesUtils;
import score.Address;

// fee2 and fee3 are the two other fees associated with the two other pools of token0 and token1
class FlashCallbackData {
    BigInteger amount0;
    BigInteger amount1;
    Address payer;
    PoolAddress.PoolKey poolKey;
    int poolFee2;
    int poolFee3;

    public FlashCallbackData (
        BigInteger amount0,
        BigInteger amount1,
        Address payer,
        PoolAddress.PoolKey poolKey,
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

    public static FlashCallbackData fromBytes(ByteReader reader) {
        int amount0Size = reader.readInt();
        BigInteger amount0 = reader.readBigInteger(amount0Size);

        int amount1Size = reader.readInt();
        BigInteger amount1 = reader.readBigInteger(amount1Size);

        Address payer = reader.readAddress();

        PoolAddress.PoolKey poolKey = PoolAddress.PoolKey.fromBytes(reader);

        int poolFee2 = reader.readInt();
        int poolFee3 = reader.readInt();

        return new FlashCallbackData(amount0, amount1, payer, poolKey, poolFee2, poolFee3);
    }

    public byte[] toBytes () {
      return BytesUtils.concat(
        BytesUtils.intToBytes(this.amount0.toByteArray().length),
        this.amount0.toByteArray(),
        
        BytesUtils.intToBytes(this.amount1.toByteArray().length),
        this.amount1.toByteArray(),

        this.payer.toByteArray(),

        this.poolKey.toBytes(),

        BytesUtils.intToBytes(this.poolFee2),
        BytesUtils.intToBytes(this.poolFee3)
      );
    }
}