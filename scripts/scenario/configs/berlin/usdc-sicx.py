import json

USDC = {
  "address": "cxa818e190782c7bc64c1ec12512c7f8f3171fc8cf",
  "amount": int(0.60 * 10**6)
}

SICX = {
  "address": "cx79ada8b605380c84507d42534080ada30c77602c",
  "amount": 10**18
}

token0, token1 = sorted([USDC, SICX], key=lambda token: token['address'])

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