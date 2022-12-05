import json

WETH = {
  "address": "cxb9d8730b23a6fa319bdfa3d5162606c2039c5156",
  "amount": int(1 * 10**18)
}

CRV = {
  "address": "cx4892e2825f7a4f95ef6c69f2a5456745d28a551a",
  "amount": int(1364 * 10**18)
}

token0, token1 = sorted([WETH, CRV], key=lambda token: token['address'])

config = {
  # Type: String
  # "network" value must be:
  #   - "lisbon" for Lisbon Network,
  #   - "lisbon" for Lisbon Network,
  #   - "custom" for the custom Convexus Network
  #   - "mainnet" for MainNet Network,
  "network": "lisbon",

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