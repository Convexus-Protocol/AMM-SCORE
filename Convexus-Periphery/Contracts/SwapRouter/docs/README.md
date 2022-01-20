# Convexus SwapRouter Contract Documentation

The `SwapRouter` is the contract doing all verifications in terms of amounts or slippage and handle the swap callback which is responsible to verify the authenticity of the pool. The SwapRouter can manage multiroute swaps.

# **Tokens Swap**

The `SwapRouter` is able to swap two tokens thanks to the [ConvexusPool `swap`](/Convexus-Core/Contracts/Pool/docs/README.md#convexuspoolswap) method. It supports multiple ways of swapping tokens, depending of the user needs:

- `exactInputSingle`: Swaps `amountIn` of one token for as much as possible of another token
- `exactInput`: Swaps `amountIn` of one token for as much as possible of another along the specified path
- `exactOutputSingle`: Swaps as little as possible of one token for `amountOut` of another token
- `exactOutput`: Swaps as little as possible of one token for `amountOut` of another along the specified path (reversed)

Below is the entire process flow for swapping a fixed amount of `token0` to a Convexus Pool, using the `exactInputSingle` method from a Convexus Swap Router. The process flow is the same for `exactInput`, `exactOutputSingle` and `exactOutput`.


> ⚠️ If you need more detailed information about the token swap process in the core layer, see [here](/Convexus-Core/Contracts/Pool/docs/README.md#tokens-swap).


![exactInputSingle](./uml/exactInputSingle.svg)

## `SwapRouter::ExactInputSingleParams`

### ⚙️ Structure definition

```java
class ExactInputSingleParams {
  // The token expected as output
  Address tokenOut;
  // The pool fee
  int fee;
  // The recipient address
  Address recipient;
  // The timestamp deadline, in seconds
  BigInteger deadline;
  // The minimum amount of token in output
  BigInteger amountOutMinimum;
  // The Q64.96 sqrt price limit
  BigInteger sqrtPriceLimitX96;
}
```

## `SwapRouter::exactInputSingle`

### 📜 Method Call

```java
// @External - this method is external through tokenFallback
private void exactInputSingle (
    Address caller, 
    Address tokenIn, 
    BigInteger amountIn, 
    ExactInputSingleParams params
)
```

- Swaps `amountIn` of one token for as much as possible of another token
- Access: Everyone
- `caller`: The method caller, it is handled by tokenFallback
- `tokenIn`: The tokenIn address, it is handled by tokenFallback
- `amountIn`: The token amount sent, it is handled by tokenFallback
- `params`: The parameters necessary for the swap, encoded as [`ExactInputSingleParams`](#swaprouterexactinputsingleparams)

### 🧪 Example call


```java
{
  "to": token0,
  "method": "transfer",
  "params": {
    "_to": SwapRouter,
    "_value": "0xde0b6b3a7640000", // 10**18
    "_data": hex({
      "method": "exactInputSingle",
      "params": {
        "tokenOut": token1,
        "fee": "0xbb8", // 3000, 0.3%
        "recipient": recipient,
        "deadline": "0x61e92f6b", // in seconds
        "amountOutMinimum": "0x1", // a low amount such as 1 so it may accept anything
        "sqrtPriceLimitX96": "0xb504f333f9de6484597d89b3" // encodePriceSqrt(1, 2)
      }
    })
  },
}
```