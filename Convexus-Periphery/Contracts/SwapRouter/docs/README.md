# Convexus SwapRouter Contract Documentation


# **Tokens Swap**

A Convexus Router is able to swap two tokens by calling the Convexus Pool `swap` method. It supports multiple ways of swapping tokens, depending of the user needs:

- `exactInputSingle`: Swaps `amountIn` of one token for as much as possible of another token
- `exactInput`: Swaps `amountIn` of one token for as much as possible of another along the specified path
- `exactOutputSingle`: Swaps as little as possible of one token for `amountOut` of another token
- `exactOutput`: Swaps as little as possible of one token for `amountOut` of another along the specified path (reversed)

Below is the entire process flow for swapping a fixed amount of `token0` to a Convexus Pool, using the `exactInputSingle` method from a Convexus Swap Router

![exactInputSingle](./uml/exactInputSingle.svg)


## 📜 `SwapRouter::exactInputSingle`

<!-- TODO -->


## 📜 `SwapRouter::convexusSwapCallback`

After calling a `ConvexusPool::swap` method, the Pool will send the computed amount of tokens to the recipient address. After doing so, it will be waiting for the correct amount of input tokens. In the `convexusSwapCallback` implementation, you must pay the pool tokens owed for the swap, or the transaction will revert.

The caller of this method must be checked to be a `ConvexusPool` deployed by the canonical `ConvexusFactory`. `amount0Delta` and `amount1Delta` can both be 0 if no tokens were swapped.

## Method Call

- Called to `msg.sender` after executing a swap via `ConvexusPool::swap`.
- Access: Everyone
- In the implementation you must pay the pool tokens owed for the swap.
- The caller of this method must be checked to be a ConvexusPool deployed by the canonical ConvexusFactory. 
- `amount0Delta` and `amount1Delta` can both be 0 if no tokens were swapped.


```java
@External
public void convexusSwapCallback (
    BigInteger amount0Delta,
    BigInteger amount1Delta,
    byte[] data
)
```

- `amount0Delta`: The amount of token0 that was sent (negative) or must be received (positive) by the pool by the end of the swap. If positive, the callback must send that amount of token0 to the pool.
- `amount1Delta`: The amount of token1 that was sent (negative) or must be received (positive) by the pool by the end of the swap. If positive, the callback must send that amount of token1 to the pool.
- `data`: Any data passed through by the caller via the swap call

### Example call:

```java
{
  "to": SwapRouter,
  "method": "convexusSwapCallback",
  "params": {
    "amount0Delta": "0x1000", // needs to be paid to the pool
    "amount1Delta": "0x0",
    "data": [...] // same value than the `swap` data field
  },
}
```
