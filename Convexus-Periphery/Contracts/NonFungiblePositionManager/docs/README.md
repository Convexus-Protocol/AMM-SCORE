# Convexus NonFungiblePositionManager Contract Documentation

Convexus NonFungiblePositionManager manages users positions within a Convexus Pool. Each position mints a NFT owned by the liquidity provider. 

# **Add Liquidity & mint a NFT position**

In order to add liquidity to a ConvexusPool, the liquidity provider needs to deposit his tokens first using the `deposit` method visible from `tokenFallback`, then call the `mint` method with its position details in order to retrieve the position NFT.

## `NonFungiblePositionManager::deposit`

### 📜 Method Call

- Deposit tokens to the `NonFungiblePositionManager`.
- Access: Everyone

```java
// @External - this method is external through tokenFallback
public void deposit (
    Address caller, 
    Address tokenIn, 
    BigInteger amountIn
)
```
- `caller`: The method caller. This field is handled by tokenFallback
- `tokenIn`: The tokenIn address. This field is handled by tokenFallback
- `amountIn` The token amount sent. This field is handled by tokenFallback

### 🧪 Example call

```java
{
  "to": tokenIn, // equivalent to tokenIn
  "method": "transfer",
  "params": {
    "_to": NonFungiblePositionManager,
    "_value": "0x21e19e0c9bab2400000", // equivalent to amountIn
    "_data": hex({
      "method": "deposit"
    })
  }
}
```


## `NonFungiblePositionManager::mint`

### 📜 Method Call

- Creates a new position wrapped in a NFT.
- Access: Everyone
- Call this when the pool does exist and is initialized. Note that if the pool is created but not initialized, the call will fail.

```java
@External
public MintResult mint (
    MintParams params
)
```

- `params`: The params necessary to mint a position, encoded as [`MintParams`](#nonfungiblepositionmanagermintparams)
- Emits a [`IncreaseLiquidity`](#nonfungiblepositionmanagerincreaseliquidity) event

### 🧪 Example call

```java
{
  "to": NonFungiblePositionManager,
  "method": "mint",
  "params": {
    "params": {
      "token0": token0,
      "token1": token1,
      "fee": "0xbb8", // 3000
      "tickLower": "-0xd89b4", // A low tick
      "tickUpper": "0xd89b4", // A high tick
      "amount0Desired": "0x21e19e0c9bab2400000", // 10000 * 10**18
      "amount1Desired": "0x21e19e0c9bab2400000", // 10000 * 10**18
      "amount0Min": "0", // Shouldn't be 0 in production
      "amount1Min": "0", // Shouldn't be 0 in production
      "recipient": recipient, // 
      "deadline": "0x61e92f6b"
    }
  }
}
```

## `NonFungiblePositionManager::MintParams`

### ⚙️ Structure definition

```java
class MintParams {
    Address token0;
    Address token1;
    int fee;
    int tickLower;
    int tickUpper;
    BigInteger amount0Desired;
    BigInteger amount1Desired;
    BigInteger amount0Min;
    BigInteger amount1Min;
    Address recipient;
    BigInteger deadline;
}
```

- `token0`: The first token of a pool, unsorted
- `token1`: The second token of a pool, unsorted
- `fee`: The fee level of the pool
- `tickLower`: The lower tick of the position
- `tickUpper`: The upper tick of the position
- `amount0Desired`: The desired amount of token0 to be spent,
- `amount1Desired`: The desired amount of token1 to be spent,
- `amount0Min`: The minimum amount of token0 to spend, which serves as a slippage check,
- `amount1Min`: The minimum amount of token1 to spend, which serves as a slippage check,
- `recipient`: The address that received the output of the swap
- `deadline`: The unix time after which a mint will fail, to protect against long-pending transactions and wild swings in prices

## `NonFungiblePositionManager::IncreaseLiquidity`

### ⚙️ EventLog definition

Emitted when liquidity is increased for a position NFT
Also emitted when a token is minted

```java
@EventLog
public void IncreaseLiquidity (
  BigInteger tokenId, 
  BigInteger liquidity, 
  BigInteger amount0,
  BigInteger amount1
)
```

- `tokenId`: The ID of the token for which liquidity was increased
- `liquidity`: The amount by which liquidity for the NFT position was increased
- `amount0`: The amount of token0 that was paid for the increase in liquidity
- `amount1`: The amount of token1 that was paid for the increase in liquidity
