# Convexus Pool Contract Documentation

# Pool initialization


## 📜 `initialize`


## Method Call

- Sets the initial price for the pool
- Access: Everyone
- Price is represented as a sqrt(amountToken1/amountToken0) [Q64.96 value](../../../Convexus-Commons/Librairies/README.md#how-to-encode-a-Q64-96-price)

```java
@External
public void initialize (BigInteger sqrtPriceX96)
```

- @param sqrtPriceX96 the initial sqrt price of the pool as a Q64.96

- Example call:

```java
{
  "to": POOL_ADDRESS,
  "method": "initialize",
  "params": {
    "sqrtPriceX96" : "0x50f44d8921243b6cdba25b3c" // encodePriceSqrt(1, 10)
  },
}
```

# Deposit liquidity to the pool

