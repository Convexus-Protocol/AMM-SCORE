import json

USDT = {
  "address": "cx1727da573c78098f1b7cd6e01ae11a3da935d2e1",
  "amount": int(0.60 * 10**6)
}

ICX = {
  "address": "cx1111111111111111111111111111111111111111",
  "amount": 10**18
}

token0, token1 = sorted([USDT, ICX], key=lambda token: token['address'])

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