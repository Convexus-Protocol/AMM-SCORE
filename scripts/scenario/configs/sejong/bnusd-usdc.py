import json

BNUSD = {
  "address": "cx5838cb516d6156a060f90e9a3de92381331ff024",
  "amount": 10**18
}

IUSDC = {
  "address": "cx599d58885e5b1736c934fca7e53e04c797ab05be",
  "amount": 10**6
}

token0, token1 = sorted([BNUSD, IUSDC], key=lambda token: token['address'])

config = {
  # Type: String
  # "network" value must be:
  #   - "lisbon" for Lisbon Network,
  #   - "lisbon" for Lisbon Network,
  #   - "custom" for the custom Convexus Network
  #   - "mainnet" for MainNet Network,
  "network": "sejong",

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
    "fee": 500
  }
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))