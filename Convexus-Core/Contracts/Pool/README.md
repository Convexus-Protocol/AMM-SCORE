# Convexus Pool Contract Documentation

---

# How to encode a Q64.96 price

- Given two amounts of tokens, the simple answer is : 

> `sqrt(amount1 / amount2) * 2**96`

- Please note that both division and square root  operations **needs** at least 128 bits decimal precision. So please use appropriate librairies supporting such precision, such as [jsbi](https://github.com/GoogleChromeLabs/jsbi).

```javascript
import JSBI from 'jsbi'
import invariant from 'tiny-invariant'

export const MAX_SAFE_INTEGER = JSBI.BigInt(Number.MAX_SAFE_INTEGER)

const ZERO = JSBI.BigInt(0)
const ONE = JSBI.BigInt(1)
const TWO = JSBI.BigInt(2)

/**
 * Computes floor(sqrt(value))
 * @param value the value for which to compute the square root, rounded down
 */
export function sqrt(value: JSBI): JSBI {
  invariant(JSBI.greaterThanOrEqual(value, ZERO), 'NEGATIVE')

  // rely on built in sqrt if possible
  if (JSBI.lessThan(value, MAX_SAFE_INTEGER)) {
    return JSBI.BigInt(Math.floor(Math.sqrt(JSBI.toNumber(value))))
  }

  let z: JSBI
  let x: JSBI
  z = value
  x = JSBI.add(JSBI.divide(value, TWO), ONE)
  while (JSBI.lessThan(x, z)) {
    z = x
    x = JSBI.divide(JSBI.add(JSBI.divide(value, x), x), TWO)
  }
  return z
}

/**
 * Returns the sqrt ratio as a Q64.96 corresponding to a given ratio of amount1 and amount0
 * @param amount1 The numerator amount i.e., the amount of token1
 * @param amount0 The denominator amount i.e., the amount of token0
 * @returns The sqrt ratio
 */

export function encodeSqrtRatioX96(amount1: JSBI , amount0: JSBI ): JSBI {
  const numerator = JSBI.leftShift(JSBI.BigInt(amount1), JSBI.BigInt(192))
  const denominator = JSBI.BigInt(amount0)
  const ratioX192 = JSBI.divide(numerator, denominator)
  return sqrt(ratioX192)
}
```


# Pool initialization


## 📜 `initialize`


## Method Call

- Sets the initial price for the pool
- Access: Everyone
- Price is represented as a sqrt(amountToken1/amountToken0) Q64.96 value

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

