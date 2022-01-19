# Convexus Pool Contract Documentation

---

# How to encode a Q64.96 price

- Given two amounts of tokens, the simple answer is : 

> `sqrt(amount1 / amount0) * 2**96`

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
