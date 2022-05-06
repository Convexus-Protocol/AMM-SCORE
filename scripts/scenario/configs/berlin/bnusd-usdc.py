import json

BNUSD = {
  "address": "cxd57fe1c5e63385f412b814634102609e8a987e3a",
  "amount": 10**18
}

USDC = {
  "address": "cx7a5be2907bc1f4d6fae0462df08e7a21972d7347",
  "amount": 10**18
}

token0, token1 = sorted([BNUSD, USDC], key=lambda token: token['address'])

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
    "fee": 500
  }
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))