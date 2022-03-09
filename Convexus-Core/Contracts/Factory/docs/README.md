# Convexus Factory Contract Documentation

Convexus Factory deploys Convexus pools and manages ownership and control over pool protocol fees.

# **Create a pool**

## `ConvexusFactory::createPool`

### 📜 Method Call

- Creates a pool for the given two tokens and fee
- Access: Everyone

```java
@External
public Address createPool (
  Address tokenA,
  Address tokenB,
  int fee
)
```

- `tokenA`: One of the two tokens in the desired pool
- `tokenB`: The other of the two tokens in the desired pool
- `fee`: The desired fee for the pool ; divide this value by 10000 to get the percent value
- tokenA and tokenB may be passed in either order: token0/token1 or token1/token0. tickSpacing is retrieved from the fee. The call will revert if the pool already exists, the fee is invalid, or the token arguments are invalid.
- The newly deployed pool will be automatically added to the global `ConvexusFactory::pools` list.

### 🧪 Example call

```java
{
  "to": ConvexusFactory,
  "method": "createPool",
  "params": {
    "tokenA": "cxcf50d121c8bdbc16437cb569863f51039480a848",
    "tokenB": "cx1eb5386b85e310dd3f0db647a7e85a5b6a015642",
    "fee": "0xbb8" // 3000 = 0.3%
  },
}
```

# **Factory settings**

## `ConvexusFactory::setOwner`

### 📜 Method Call

- Updates the owner of the factory
- Access: Current owner

```java
@External
public void setOwner (
  Address _owner
)
```

- `_owner`: The new owner of the factory

### 🧪 Example call

```java
{
  "to": ConvexusFactory,
  "method": "setOwner",
  "params": {
    "_owner": "cxcf50d121c8bdbc16437cb569863f51039480a848", // a multisig contract
  },
}
```

## `ConvexusFactory::enableFeeAmount`

### 📜 Method Call

- Enables a fee amount with the given tickSpacing
- Fee amounts may never be removed once enabled
- Access: Current owner

```java
@External
public void enableFeeAmount (
  int fee, 
  int tickSpacing
)
```

- `fee`: The fee amount to enable, denominated in hundredths of a bip (i.e. 1e-6)
- `tickSpacing`: The spacing between ticks to be enforced for all pools created with the given fee amount
- Requirements:
  - `fee` needs to be lower than 1000000
  - tick spacing is capped at 16384 to prevent the situation where tickSpacing is so large that `TickBitmap#nextInitializedTickWithinOneWord` overflows

### 🧪 Example call

```java
{
  "to": ConvexusFactory,
  "method": "enableFeeAmount",
  "params": {
    "fee": "0xbb8", // 0.3%
    "tickSpacing": "0x36b0", // 14000
  },
}
```


## `ConvexusFactory::setPoolContract`

### 📜 Method Call

- Set the pool contract bytes to be newly deployed with `createPool`
- Access: Current owner

```java
@External
public void setPoolContract (
  byte[] contractBytes
)
```

- `contractBytes`: The contract bytes to be deployed - It should be a JAR file bytes in Java-SCORE

### 🧪 Example call

```java
{
  "to": ConvexusFactory,
  "method": "setPoolContract",
  "params": {
    "contractBytes": "0xaabbccddee...", // the JAR file bytes
  },
}
```


## `ConvexusFactory::owner`

### 📜 Method Call

- Get the current owner of the Factory
- Access: Everyone

```java
@External(readonly = true)
public Address owner()
```


## `ConvexusFactory::poolContract`

### 📜 Method Call

- Get the current pool contract bytes of the Factory
- Access: Everyone

```java
@External(readonly = true)
public byte[] poolContract ()
```


## `ConvexusFactory::poolsSize`

### 📜 Method Call

- Get the deployed pools list size
- Access: Everyone

```java
@External(readonly = true)
public byte[] poolsSize ()
```


## `ConvexusFactory::pools`

### 📜 Method Call

- Get a deployed pools list item
- Access: Everyone

```java
@External(readonly = true)
public Address pools(int index)
```

- `index`: the index of the item to read from the deployed pools list


## `ConvexusFactory::getPool`

### 📜 Method Call

- Get a deployed pool address from its parameters
- Access: Everyone

```java
@External(readonly = true)
public Address getPool (
  Address token0, 
  Address token1, 
  int fee
)
```

- `token0`: One of the two tokens in the desired pool
- `token1`: The other of the two tokens in the desired pool
- `fee`: The desired fee for the pool ; divide this value by 10000 to get the percent value
- The `token0` and `token1` parameters can be inverted, it will return the same pool address