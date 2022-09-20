import json

WETH = {
  "address": "cx01377f9854bd10d4de01e244f74d39c5d553c51d",
  "amount": int(1 * 10**18)
}

CRV = {
  "address": "cx0a9cd8e1c3de89a59f34841c3538db36b09d1f3b",
  "amount": int(1364 * 10**18)
}

token0, token1 = sorted([WETH, CRV], key=lambda token: token['address'])

config = {
  # Type: String
  # "network" value must be:
  #   - "lisbon" for Lisbon Network,
  #   - "berlin" for Berlin Network,
  #   - "custom" for the custom Convexus Network
  #   - "mainnet" for MainNet Network,
  "network": "berlin",

  "pool": {
    # Type: Address
    # token0 address
    "token0": token0['address'],
    # Type: BigInteger
    # Amount0 for initialization
    "amount0": token0['amount'],

    # Type: Address
    # token1 address
    "token1": token1['address'],
    # Type: BigInteger
    # Amount1 for initialization
    "amount1": token1['amount'],

    # Type: Integer
    "fee": 3000
  }
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))