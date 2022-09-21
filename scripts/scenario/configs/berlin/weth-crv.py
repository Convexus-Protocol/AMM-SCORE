import json

WETH = {
  "address": "cx1126c5dc7115daea7f55d6b6cf0eb63adeb3529f",
  "amount": int(1 * 10**18)
}

CRV = {
  "address": "cxc8373f6f2654a9c8b689059aef58aefb9f878e12",
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