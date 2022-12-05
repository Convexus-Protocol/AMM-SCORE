import json

BNUSD = {
  "address": "cx43f9902890ce7ff01a4cda809337bdadc5dcf849",
  "amount": 10**18
}

USDC = {
  "address": "cx71698bfaa0fc2c8929c226c52611ba033eaafc76",
  "amount": 10**6
}

token0, token1 = sorted([BNUSD, USDC], key=lambda token: token['address'])

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
    "fee": 500
  }
}

# This script must only print the configuration in JSON format, nothing else
print(json.dumps(config))