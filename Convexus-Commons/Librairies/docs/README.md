# Convexus Librairies Documentation

---

# What is Q64.96 price ?

In Convexus, prices of tokens are stored in the 0th slot of the pool state. Storing the price values instead of deriving them allows pools to perform higher precision operations. The price is stored as a square root because of the geometric nature of the core AMM algorithm, x*y=k. Essentially, the math works out well when working with the square root of the price.

In addition, you'll notice the X96 suffix at the end of the variable name. This X* naming convention is used throughout the Convexus codebase to indicate values that are encoded as binary fixed-point numbers. Fixed-point is excellent at representing fractions while maintaining consistent fidelity and high precision in integer-only environments like SCORE, making it a perfect fit for representing prices, which of course are ultimately fractions. The number after X indicates the number of fraction bits - 96 in this case - reserved for encoding the value after the decimal point. The number of integer bits can be trivially derived from the size of the variable and the number of fraction bits. In this case, sqrtPriceX96 is stored as a uint160, meaning that there are 160 - 96 = 64 integer bits.


# How to encode a Q64.96 price

- Given two amounts of tokens, the simple answer is : 

> `sqrt(price) * 2**96`

or 

> `sqrt(amount1 / amount0) * 2**96`

- Please note that both division and square root  operations **needs** at least 128 bits decimal precision. Please use appropriate librairies supporting such precision, such as [jsbi](https://github.com/GoogleChromeLabs/jsbi).

Here's an example below in Javascript:

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
function sqrt(value) {
  invariant(JSBI.greaterThanOrEqual(value, ZERO), 'NEGATIVE')

  // rely on built in sqrt if possible
  if (JSBI.lessThan(value, MAX_SAFE_INTEGER)) {
    return JSBI.BigInt(Math.floor(Math.sqrt(JSBI.toNumber(value))))
  }

  let z = value
  let x = JSBI.add(JSBI.divide(value, TWO), ONE)
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

export function encodePriceSqrt(amount1, amount0) {
  const numerator = JSBI.leftShift(JSBI.BigInt(amount1), JSBI.BigInt(192))
  const denominator = JSBI.BigInt(amount0)
  const ratioX192 = JSBI.divide(numerator, denominator)
  return sqrt(ratioX192)
}

console.assert(String(encodePriceSqrt(1, 1))  == "79228162514264337593543950336")
console.assert(String(encodePriceSqrt(1, 10)) == "25054144837504793118641380156")
```

Exemple:
```java
amount0 = 10
amount1 = 1
sqrtPriceX96 = sqrt(amount1 / amount0) * 2**96
sqrtPriceX96 = sqrt(0.1) * 2**96
sqrtPriceX96 = 25054144837504793118641380156
```

# How to decode a Q64.96 to a floating point price

- The simple answer is : 

> `price = sqrtPriceX96**2 / 2**192`

The math for retrieving a price from a Q64.96 are pretty simple: 

```java
sqrtPriceX96 = sqrt(price) * 2 ** 96
// divide both sides by 2 ** 96
sqrtPriceX96 / (2 ** 96) = sqrt(price)
// square both sides
(sqrtPriceX96 / (2 ** 96)) ** 2 = price
// expand the squared fraction
(sqrtPriceX96 ** 2) / ((2 ** 96) ** 2)  = price
// multiply the exponents in the denominator to get the final expression
sqrtPriceX96 ** 2 / 2 ** 192 = price
```

Exemple:
```java
sqrtPriceX96 = 25054144837504793118641380156 // encodePriceSqrt(1, 10)
price = sqrtPriceX96**2 / 2**192
price = 0.1
```

---

# How to encode a Swap path

A **path** if a sequence of [`tokenIn`, `fee`, `tokenOut`], encoded in bytes.

The `tokenIn` and `tokenOut` addresses must be represented in bytes using its standard format provided in the ICON SDK.
The `fee` must be represented in bytes as a big endian 32-bits integer.

Here's an example:

Let's say we want to swap some tokenA to tokenB, then tokenB to tokenC, using a 0.3% fee tier.

- `tokenA` = `cx000000000000000000000000000000000000000a`
- `tokenB` = `cx000000000000000000000000000000000000000b`
- `tokenC` = `cx000000000000000000000000000000000000000c`
- `fee` = `3000`

Converted to bytes:

- `tokenA` = `01000000000000000000000000000000000000000A`
- `tokenB` = `01000000000000000000000000000000000000000B`
- `tokenC` = `01000000000000000000000000000000000000000C`
- `fee` = `00000BB8`

The path format will be: 

> `[tokenA, fee, tokenB, fee, tokenC]`

Hence the final path will be:

`01000000000000000000000000000000000000000A00000BB801000000000000000000000000000000000000000B00000BB801000000000000000000000000000000000000000C`